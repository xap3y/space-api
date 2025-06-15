package me.xap3y.space.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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
            public void addCorsMappings(CorsRegistry registry) {

                registry.addMapping("/**")
                        .allowedOrigins(
                                "https://xap3y.space",
                                "https://s.xap3y.tech",
                                "https://space.xap3y.tech",
                                "https://space.xap3y.fun",
                                "https://s.xap3y.fun",
                                "https://test.xap3y.tech",
                                "https://call.xap3y.tech",
                                "https://api.xap3y.tech",
                                "http://localhost:3000",
                                "http://localhost:3006",
                                "http://192.168.100.100:3000",
                                "https://ext-space.xap3y.tech",
                                "https://space0.xap3y.tech",
                                "https://space1.xap3y.tech",
                                serverInfo.getTestCorsUrl()
                        )
                        .allowCredentials(true)
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/static/**")
                        .addResourceLocations("classpath:/static/");
            }
        };
    }
}
