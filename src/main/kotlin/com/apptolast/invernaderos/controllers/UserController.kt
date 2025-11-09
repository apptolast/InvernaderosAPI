package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.metadata.entity.User
import com.apptolast.invernaderos.repositories.metadata.UserRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userRepository: UserRepository
) {


    @GetMapping("/users")
    fun getAllUsers(): List<User> {

        return userRepository.findAll()
    }

}