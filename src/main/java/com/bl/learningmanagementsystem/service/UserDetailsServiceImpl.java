package com.bl.learningmanagementsystem.service;

import java.time.LocalDateTime;
import java.util.ArrayList;

import com.bl.learningmanagementsystem.dto.JwtRequestDto;
import com.bl.learningmanagementsystem.dto.LoginDto;
import com.bl.learningmanagementsystem.dto.UserDto;
import com.bl.learningmanagementsystem.exception.LmsAppServiceException;
import com.bl.learningmanagementsystem.repository.UserRepository;
import com.bl.learningmanagementsystem.util.JwtTokenUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bl.learningmanagementsystem.model.User;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;

@Service
public class UserDetailsServiceImpl implements UserDetailsService, IUserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder bcryptEncoder;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByFirstName(username).orElseThrow(() -> new LmsAppServiceException(LmsAppServiceException.exceptionType
                .USER_NOT_FOUND, "User not found with username"));
        return new org.springframework.security.core.userdetails.User(user.getFirstName(), user.getPassword(),
                new ArrayList<>());
    }

    @Override
    public long loginUser(LoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.email).orElseThrow(() -> new LmsAppServiceException(LmsAppServiceException.exceptionType
                .INVALID_EMAIL_ID, "User not found with email"));
        if (!bcryptEncoder.matches(loginDto.password, user.getPassword()))
            throw new LmsAppServiceException(LmsAppServiceException.exceptionType.INVALID_PASSWORD, "Invalid password");
        return user.getId();
    }

    @Override
    public User save(UserDto user) {
        user.setCreatorStamp(LocalDateTime.now());
        user.setCreatorUser(user.getFirstName());
        user.setVerified("yes");
        user.setPassword(bcryptEncoder.encode(user.getPassword()));
        User newUser = modelMapper.map(user, User.class);
        return userRepository.save(newUser);
    }

    @Override
    public User resetPassword(String password, String token) {

        String encodedPassword = bcryptEncoder.encode(password);
        if (jwtTokenUtil.isTokenExpired(token)) {
            throw new LmsAppServiceException(LmsAppServiceException.exceptionType.INVALID_TOKEN, "Token expired");
        }
        long id = Long.valueOf(jwtTokenUtil.getSubjectFromToken(token));
        return userRepository.findById(id)
                .map(user -> {
                    user.setPassword(encodedPassword);
                    return user;
                })
                .map(userRepository::save).get();
    }

    @Override
    public String getResetPasswordToken(String email) throws MessagingException, LmsAppServiceException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new LmsAppServiceException(LmsAppServiceException.exceptionType
                .INVALID_EMAIL_ID, "User not found with email"));
        final String token = jwtTokenUtil.generatePasswordResetToken(String.valueOf(user.getId()));
        emailService.sentEmail(user, token);
        return token;
    }

    @Override
    public String getAuthenticationToken(JwtRequestDto authenticationRequest) throws Exception {
        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        final UserDetails userDetails = userDetailsService
                .loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);
        return token;
    }


    private void authenticate(String username, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}
