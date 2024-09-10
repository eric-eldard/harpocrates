package com.eric_eldard.harpocrates.persistence;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.eric_eldard.harpocrates.annotation.DataClassification;
import com.eric_eldard.harpocrates.model.DataDefinition;
import com.eric_eldard.harpocrates.util.FunctionUtils;

/**
 * This class searches for your entity fields annotated with {@link DataClassification} and then URL-encodes that info
 * into the comment area of the corresponding MySQL table column.
 * <br><br>
 * <b>Spring setup:</b>
 * <ul>
 *     <li>add a bean of the type {@link DataClassifier} to your Spring context</li>
 *     <li>ensure a {@link DataSource} bean is available in your Spring context</li>
 *     <li>set the property {@code harpocrates.base-package-to-scan}</li>
 * </ul>
 * {@link DataClassifier#writeClassificationsToDb()} will run automatically on startup
 * <br><br>
 * <b>Manual setup:</b>
 * <pre>
 * new DataClassifierImpl(dataSource, "my.base.package").writeClassificationsToDb();
 * </pre>
 */
@Component
public class DataClassifierImpl implements DataClassifier
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DataClassifierImpl.class);

    private static final String GET_TABLE_DEF = "SHOW CREATE TABLE `%s`;";

    private static final String COMMENT_KEY = "dataClassification";

    /**
     * Match a column definition with optional COMMENT and optional trailing ",", and capture 3 groups:
     * <ul>
     *     <li>the column name, without surrounding backticks</li>
     *     <li>the column definition, without the COMMENT statement or trailing comma</li>
     *     <li>any comment, without the COMMENT statement and without surrounding single quotes</li>
     * </ul>
     * <a href="https://regex101.com/r/ifiUpx/1">Debug this regex</a>
     */
    private static final Pattern COL_NAME_PATTERN =
        Pattern.compile("^ *`(.+)` ((?:(?!COMMENT|,).)+),?(?:COMMENT ')?((?:(?!').)*)'?,?$");

    private static final Splitter.MapSplitter COMMENT_SPLITTER = Splitter.on(',').withKeyValueSeparator('=');

    private final DataSource dataSource;

    private final JdbcTemplate jdbcTemplate;

    private final String basePackageToScan;

    private final boolean destroyAfterExecution;

    private BeanFactory beanFactory;

    /**
     * Non-Spring constructor
     */
    public DataClassifierImpl(DataSource dataSource, String basePackageToScan)
    {
        this(dataSource, basePackageToScan, false);
    }

    /**
     * @param dataSource            A datasource with access to modify the tables which store entities with
     *                              {@link DataClassification}s
     *                              <br>
     * @param basePackageToScan     The lowest level classpath package to start recursively scanning for
     *                              {@link DataClassification}s
     *                              <br>
     * @param destroyAfterExecution If {@code true} and this instance is a Spring bean,
     *                              {@link DefaultListableBeanFactory#removeBeanDefinition(String)} will be invoked on
     *                              this {@link DataClassifier} after execution. DataClassifier runs on app startup, and
     *                              there is generally no reason to keep it around after it's written classifications to
     *                              your database.
     */
    @Autowired
    @SneakyThrows
    private DataClassifierImpl(@Qualifier("${harpocrates.datasource:dataSource}") DataSource dataSource,
                              @Value("${harpocrates.base-package-to-scan}") String basePackageToScan,
                              @Value("${harpocrates.destroy-after-exec:true}") boolean destroyAfterExecution
    )
    {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.basePackageToScan = basePackageToScan;
        this.destroyAfterExecution = destroyAfterExecution;

        try (Connection tmpConn = dataSource.getConnection())
        {
            LOGGER.info("\n\n*** Harpocrates data classifier started ***" +
                "\n- database:\t\t\t\t\t" + tmpConn.getMetaData().getURL() +
                "\n- base package to scan:\t\t" + basePackageToScan +
                "\n- destroy DataClassifier:\t" + destroyAfterExecution + '\n'
            );
        }
    }

    @PostConstruct
    public void writeClassificationsToDb()
    {
        Set<BeanDefinition> classesWithDataClassification = getClassesWithDataClassifications();

        LOGGER.info("Harpocrates found {} {} with data classifications",
            classesWithDataClassification.size(),
            classesWithDataClassification.size() == 1 ? "class" : "classes"
        );

        for (BeanDefinition candidateClass : classesWithDataClassification)
        {
            String className = candidateClass.getBeanClassName();
            Class<?> clazz = forClass(className);
            String tableName = getTableName(clazz);
            String tableDef = getTableDef(tableName);
            Map<String, DataDefinition> dataDefsByField = getDataDefsByField(clazz);

            LOGGER.info("Retrieved table definition for `{}`:\n\n{}\n", tableName, tableDef);

            List<String> columnList = Arrays.stream(tableDef.split("\n"))
                .filter(this::isColumnDef)
                .toList();

            StringBuilder alterStmt = new StringBuilder(tableDef.length());
            alterStmt.append("ALTER TABLE `").append(tableName).append('`');

            for (int i = 0; i < columnList.size(); i++)
            {
                String stmt = columnList.get(i);
                Matcher matcher = COL_NAME_PATTERN.matcher(stmt);
                if (matcher.find())
                {
                    String columnName = matcher.group(1);
                    DataDefinition dataDefinition = dataDefsByField.get(columnName);

                    if (dataDefinition != null)
                    {
                        LOGGER.debug(
                            "Preparing MODIFY statement for the following `{}` column def:\n{}\n",
                            tableName,
                            matcher.group(0)
                        );

                        String colDef = matcher.group(2);
                        String oldComment = matcher.group(3);
                        String newComment = makeNewComment(oldComment, dataDefinition);
                        boolean isLast = i == columnList.size() - 1;

                        String modifyStmt = makeModifyStatement(columnName, colDef, newComment, isLast);

                        alterStmt.append(modifyStmt);

                        LOGGER.debug("Added MODIFY statement for `{}`:{}\n", tableName, modifyStmt);
                    }
                    else
                    {
                        LOGGER.debug(
                            "The `{}` column in this statement has no data classification and won't be modified:\n{}\n",
                            tableName,
                            matcher.group(0)
                        );
                    }
                }
                else
                {
                    LOGGER.warn("The following column definition was incorrectly flagged for processing:\n{}\n", stmt);
                }
            }

            // We're guaranteed to have at least one MODIFY at this point, so we will definitely fun the ALTER statement
            String finalAlterStmt = alterStmt.toString();
            LOGGER.info("Updating table definition for `{}`:\n\n{}\n", tableName, finalAlterStmt);
            updateTable(finalAlterStmt);
        }

        shutdown();
    }

    private Set<BeanDefinition> getClassesWithDataClassifications()
    {
        ClassPathScanningCandidateComponentProvider classPathScanner =
            new ClassPathScanningCandidateComponentProvider(false);

        classPathScanner.addIncludeFilter(new FieldAnnotationTypeFilter(DataClassification.class));
        return classPathScanner.findCandidateComponents(basePackageToScan);
    }

    private Map<String, DataDefinition> getDataDefsByField(Class<?> clazz)
    {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(DataClassification.class))
            .collect(Collectors.toUnmodifiableMap(
                this::getColName,
                this::makeDataDef,
                FunctionUtils::chooseFirst
            ));
    }

    private String getTableName(Class<?> clazz)
    {
        Table tableAnnotation = AnnotationUtils.findAnnotation(clazz, Table.class);

        String tableName;
        if (tableAnnotation == null)
        {
            tableName = clazz.getSimpleName();
        }
        else
        {
            tableName = tableAnnotation.name();
        }
        return tableName;
    }

    private String getColName(Field field)
    {
        Column colAnnotation = AnnotationUtils.findAnnotation(field, Column.class);

        String colName;
        if (colAnnotation == null)
        {
            colName = field.getName();
        }
        else
        {
            colName = colAnnotation.name();
        }
        return colName;
    }

    /**
     * We use Spring's AnnotationUtils here instead of vanilla reflection because we're supporting {@link AliasFor}
     * <br>
     * @param field - must be guaranteed that we'll find a {@link DataClassification} annotation on this field
     */
    private DataDefinition makeDataDef(Field field)
    {
        DataClassification dataClassification = AnnotationUtils.findAnnotation(field, DataClassification.class);
        return DataDefinition.of(
            dataClassification.type(),
            dataClassification.action(),
            dataClassification.pattern(),
            dataClassification.description()
        );
    }

    @SneakyThrows
    private String getTableDef(String tableName)
    {
        // Can't JDBC-template-parameterize this because it adds single quotes we don't want
        String query = String.format(GET_TABLE_DEF, tableName);
        return (String) jdbcTemplate.queryForMap(query).get("Create Table");
    }

    private static String makeNewComment(String oldComment, DataDefinition dataDefinition)
    {
        Map<String, String> commentMap;

        if (Strings.isNullOrEmpty(oldComment))
        {
            commentMap = new LinkedHashMap<>(1);
        }
        else
        {
            // Splitter.MapSplitter#split returns an ordered, but unmodifiable, map.
            // This result may include other data that is not part of the classification, and we need to preserve that.
            commentMap = new LinkedHashMap<>(COMMENT_SPLITTER.split(oldComment));
        }

        String encodedJson = URLEncoder.encode(dataDefinition.toJson(), Charset.defaultCharset());

        commentMap.put(COMMENT_KEY, encodedJson); // add data definition or overwrite existing

        return commentMap.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(","));
    }

    private static String makeModifyStatement(String columnName, String colDef, String newComment, boolean isLast)
    {
        return String.format(
            "\nMODIFY COLUMN `%s` %s COMMENT '%s'%s",
            columnName,
            colDef,
            newComment,
            isLast ? ';' : ','
        );
    }

    @SneakyThrows
    private void updateTable(String alterStmt)
    {
        try (PreparedStatement statement = dataSource.getConnection().prepareStatement(alterStmt))
        {
            statement.execute();
        }
    }

    private boolean isColumnDef(String stmt)
    {
        return stmt.matches("^ *`.+`.*");
    }

    private void shutdown()
    {
        if (beanFactory instanceof DefaultListableBeanFactory listableBeanFactory)
        {
            LOGGER.info("Shutting down Harpocrates...");
            if (destroyAfterExecution)
            {
                listableBeanFactory.getBeansOfType(DataClassifierImpl.class).entrySet().stream()
                    .filter(entry -> entry.getValue().equals(this))
                    .map(Map.Entry::getKey)
                    .forEach(listableBeanFactory::removeBeanDefinition);
                LOGGER.info("Harpocrates DataClassifier Spring bean destroyed");
            }
            else
            {
                LOGGER.info("The Harpocrates DataClassifier bean remains in the Spring context. To automatically " +
                    "remove it, set property [harpocrates.destroy-after-exec=true] (which is the default value).");
            }
        }
        else
        {
            LOGGER.info("Shutting down Harpocrates; this instance was not Spring-managed.");
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }

    /**
     * {@link ClassPathScanningCandidateComponentProvider} doesn't introspect fields by default, so let's give it that
     * ability.
     */
    private class FieldAnnotationTypeFilter extends AnnotationTypeFilter
    {
        public FieldAnnotationTypeFilter(Class<? extends Annotation> annotationType)
        {
            super(annotationType);
        }

        @Override
        protected boolean matchSelf(MetadataReader metadataReader)
        {
            String className = metadataReader.getClassMetadata().getClassName();
            Field[] fields = forClass(className).getDeclaredFields();

            return Arrays.stream(fields)
                .map(Field::getDeclaredAnnotations)
                .flatMap(Stream::of)
                .map(Annotation::annotationType)
                .anyMatch(annotationClass -> annotationClass.equals(getAnnotationType()));
        }
    }

    @SneakyThrows
    private Class<?> forClass(String className)
    {
        return Class.forName(className, false, getClass().getClassLoader());
    }
}