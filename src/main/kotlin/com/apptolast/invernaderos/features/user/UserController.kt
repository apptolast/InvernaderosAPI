package com.apptolast.invernaderos.features.user

import com.apptolast.invernaderos.features.user.User
import com.apptolast.invernaderos.features.user.UserRepository
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