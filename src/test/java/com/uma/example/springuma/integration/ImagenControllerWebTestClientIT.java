package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        testClient.post()
                .uri("/medico")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated();

        medico = testClient.get()
                .uri("/medico/dni/" + medico.getDni())
                .exchange()
                .expectStatus().isOk()
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

        testClient.post()
                .uri("/paciente")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated();

        Paciente[] pacientes = testClient.get()
                .uri("/paciente/medico/" + medico.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Paciente[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(pacientes);
        assertTrue(pacientes.length > 0);

        paciente = pacientes[0];

        assertNotNull(paciente);
        assertNotNull(paciente.getId());
    }

    private Imagen subirImagen(String nombreArchivo) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part(
                "image",
                new FileSystemResource(Paths.get("src/test/resources/" + nombreArchivo).toFile())
        );

        builder.part("paciente", paciente)
                .contentType(MediaType.APPLICATION_JSON);

        testClient.post()
                .uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().is2xxSuccessful();

        Imagen[] imagenes = testClient.get()
                .uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Imagen[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(imagenes);
        assertTrue(imagenes.length > 0);

        Imagen imagen = imagenes[imagenes.length - 1];

        assertNotNull(imagen);
        assertNotNull(imagen.getId());

        return imagen;
    }

    @Test
    @DisplayName("Debe subir una imagen correctamente")
    void debeSubirImagenCorrectamente() {
        Imagen imagen = subirImagen("healthy.png");

        assertNotNull(imagen);
        assertNotNull(imagen.getId());
    }

    @Test
    @DisplayName("Debe obtener la información de una imagen")
    void debeObtenerInformacionDeImagen() {
        Imagen imagen = subirImagen("healthy.png");

        testClient.get()
                .uri("/imagen/info/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Imagen.class);
    }

    @Test
    @DisplayName("Debe descargar una imagen")
    void debeDescargarImagen() {
        Imagen imagen = subirImagen("healthy.png");

        testClient.get()
                .uri("/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class);
    }

    @Test
    @DisplayName("Debe listar las imágenes de un paciente")
    void debeListarImagenesDePaciente() {
        subirImagen("healthy.png");

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
        Imagen imagen = subirImagen("healthy.png");

        testClient.get()
                .uri("/imagen/predict/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class);
    }

    @Test
    @DisplayName("Debe eliminar una imagen")
    void debeEliminarImagen() {
        Imagen imagen = subirImagen("healthy.png");

        testClient.delete()
                .uri("/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().is2xxSuccessful();
    }
}