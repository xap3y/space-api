package me.xap3y.space.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    ServerInfo serverInfo;

    public SecurityConfig(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                /*.csrf((csrf) -> csrf
                        .ignoringRequestMatchers(
                                "/v1/image/upload",
                                "/v1/paste/create"
                        )
                )*/
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/private/*").authenticated()
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {

                registry.addMapping("/**")
                        .allowedOrigins(
                                /*"https://xap3y.space",
                                "https://space.xap3y.space",
                                "https://test.xap3y.space",
                                "https://call.xap3y.space",
                                "https://r2.xap3y.space",
                                "https://r3.xap3y.space",
                                "https://api.xap3y.space",*/
                                "http://localhost:3000",
                                "http://localhost:3006",
                                "http://192.168.100.100:3000",
                                "https://space.xap3y.eu",
                                "https://ext-space.xap3y.eu",
                                "https://temp-mail-space.xap3y.eu",
                                "https://temp-mail.xap3y.eu",
                                "https://cf-email-temp.xap3y.eu",
                                serverInfo.getTestCorsUrl()
                        )
                        .allowCredentials(true)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*");
            }

            @Override
            public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/static/**")
                        .addResourceLocations("classpath:/static/");
            }
        };
    }
}
