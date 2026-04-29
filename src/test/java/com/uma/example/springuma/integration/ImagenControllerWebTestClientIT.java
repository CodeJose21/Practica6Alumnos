package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ImagenControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;

    private Paciente paciente;
    private Medico medico;

    @PostConstruct
    public void init() {
        testClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMillis(300000))
                .build();
    }

    @BeforeEach
    void setUp() {

        medico = new Medico();
        medico.setNombre("Miguel");
        medico.setDni("835");
        medico.setEspecialidad("Ginecologo");

        medico = testClient.post()
                .uri("/medico")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Medico.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(medico);
        assertNotNull(medico.getId());

        paciente = new Paciente();
        paciente.setNombre("Maria");
        paciente.setDni("888");
        paciente.setEdad(20);
        paciente.setCita("Ginecologia");
        paciente.setMedico(medico);

        paciente = testClient.post()
                .uri("/paciente")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Paciente.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(paciente);
        assertNotNull(paciente.getId());
    }

    private Imagen subirImagen(String nombreArchivo) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part(
                "image",
                new FileSystemResource(Paths.get("src/test/resources/" + nombreArchivo).toFile())
        );

        builder.part("paciente", paciente);

        Imagen imagen = testClient.post()
                .uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Imagen.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(imagen);
        assertNotNull(imagen.getId());

        return imagen;
    }

    @Test
    @DisplayName("Debe subir una imagen correctamente")
    void debeSubirImagenCorrectamente() {
        Imagen imagen = subirImagen("test-image.png");

        assertNotNull(imagen);
        assertNotNull(imagen.getId());
    }

    @Test
    @DisplayName("Debe obtener la información de una imagen")
    void debeObtenerInformacionDeImagen() {
        Imagen imagen = subirImagen("test-image.png");

        testClient.get()
                .uri("/imagen/info/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Imagen.class);
    }

    @Test
    @DisplayName("Debe descargar una imagen")
    void debeDescargarImagen() {
        Imagen imagen = subirImagen("test-image.png");

        testClient.get()
                .uri("/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class);
    }

    @Test
    @DisplayName("Debe listar las imágenes de un paciente")
    void debeListarImagenesDePaciente() {
        subirImagen("test-image.png");

        testClient.get()
                .uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }

    @Test
    @DisplayName("Debe realizar una predicción sobre una imagen")
    void debeRealizarPrediccionSobreImagen() {
        Imagen imagen = subirImagen("test-image.png");

        testClient.get()
                .uri("/imagen/predict/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class);
    }

    @Test
    @DisplayName("Debe eliminar una imagen")
    void debeEliminarImagen() {
        Imagen imagen = subirImagen("test-image.png");

        testClient.delete()
                .uri("/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().isOk();
    }
}