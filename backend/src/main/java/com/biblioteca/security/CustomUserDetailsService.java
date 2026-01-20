package com.biblioteca.security;

import com.biblioteca.model.Socio;
import com.biblioteca.repository.SocioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Primary
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private SocioRepository socioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Socio socio = socioRepository.findByUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new org.springframework.security.core.userdetails.User(
                socio.getUsuario(),
                socio.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + socio.getRol())));
    }
}
