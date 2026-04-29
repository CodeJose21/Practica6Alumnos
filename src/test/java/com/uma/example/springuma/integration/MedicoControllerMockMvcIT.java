package com.uma.example.springuma.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;

public class MedicoControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Medico medico;

    @BeforeEach
    void setUp() {
        medico = new Medico();
        medico.setDni("835");
        medico.setNombre("Miguel");
        medico.setEspecialidad("Ginecologia");
    }

    private Medico crearMedico(Medico medico) throws Exception {
        this.mockMvc.perform(post("/medico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());

        String respuesta = this.mockMvc.perform(get("/medico/dni/" + medico.getDni()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.dni").value(medico.getDni()))
                .andExpect(jsonPath("$.nombre").value(medico.getNombre()))
                .andExpect(jsonPath("$.especialidad").value(medico.getEspecialidad()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Medico medicoCreado = objectMapper.readValue(respuesta, Medico.class);

        assertNotNull(medicoCreado);
        assertNotNull(medicoCreado.getId());

        return medicoCreado;
    }

    @Test
    @DisplayName("Debe crear un médico correctamente")
    void debeCrearMedicoCorrectamente() throws Exception {
        Medico medicoCreado = crearMedico(medico);

        assertNotNull(medicoCreado);
        assertNotNull(medicoCreado.getId());
    }

    @Test
    @DisplayName("Debe obtener un médico por ID")
    void debeObtenerMedicoPorId() throws Exception {
        Medico medicoCreado = crearMedico(medico);

        this.mockMvc.perform(get("/medico/" + medicoCreado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(medicoCreado.getId()))
                .andExpect(jsonPath("$.dni").value("835"))
                .andExpect(jsonPath("$.nombre").value("Miguel"))
                .andExpect(jsonPath("$.especialidad").value("Ginecologia"));
    }

    @Test
    @DisplayName("Debe obtener un médico por DNI")
    void debeObtenerMedicoPorDni() throws Exception {
        Medico medicoCreado = crearMedico(medico);

        this.mockMvc.perform(get("/medico/dni/" + medicoCreado.getDni()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(medicoCreado.getId()))
                .andExpect(jsonPath("$.dni").value("835"))
                .andExpect(jsonPath("$.nombre").value("Miguel"))
                .andExpect(jsonPath("$.especialidad").value("Ginecologia"));
    }

    @Test
    @DisplayName("Debe actualizar un médico correctamente")
    void debeActualizarMedicoCorrectamente() throws Exception {
        Medico medicoCreado = crearMedico(medico);

        medicoCreado.setNombre("Miguel Actualizado");
        medicoCreado.setEspecialidad("Oncologia");

        this.mockMvc.perform(put("/medico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medicoCreado)))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/medico/" + medicoCreado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(medicoCreado.getId()))
                .andExpect(jsonPath("$.nombre").value("Miguel Actualizado"))
                .andExpect(jsonPath("$.especialidad").value("Oncologia"));
    }

    @Test
    @DisplayName("Debe eliminar un médico correctamente")
    void debeEliminarMedicoCorrectamente() throws Exception {
        Medico medicoCreado = crearMedico(medico);

        this.mockMvc.perform(delete("/medico/" + medicoCreado.getId()))
                .andExpect(status().isOk());
    }
}