package com.eric_eldard.harpocrates.demo.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eric_eldard.harpocrates.demo.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>
{
}