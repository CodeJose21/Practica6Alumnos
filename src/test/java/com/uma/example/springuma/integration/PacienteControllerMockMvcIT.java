package com.uma.example.springuma.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

public class PacienteControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Paciente paciente;
    private Medico medico;

    @BeforeEach
    void setUp() {
        medico = new Medico();
        medico.setNombre("Miguel");
        medico.setDni("835");
        medico.setEspecialidad("Ginecologo");

        paciente = new Paciente();
        paciente.setNombre("Maria");
        paciente.setDni("888");
        paciente.setEdad(20);
        paciente.setCita("Ginecologia");
    }

    private Medico crearMedico(Medico medico) throws Exception {
        this.mockMvc.perform(post("/medico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());

        String respuesta = this.mockMvc.perform(get("/medico/dni/" + medico.getDni()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Medico medicoCreado = objectMapper.readValue(respuesta, Medico.class);

        assertNotNull(medicoCreado);
        assertNotNull(medicoCreado.getId());

        return medicoCreado;
    }

    private Paciente crearPaciente(Paciente paciente, Medico medicoCreado) throws Exception {
        paciente.setMedico(medicoCreado);

        this.mockMvc.perform(post("/paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isCreated());

        String respuestaLista = this.mockMvc.perform(get("/paciente/medico/" + medicoCreado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Paciente> pacientes = objectMapper.readValue(
                respuestaLista,
                new TypeReference<List<Paciente>>() {}
        );

        assertNotNull(pacientes);

        return pacientes.stream()
                .filter(p -> paciente.getDni().equals(p.getDni()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró el paciente creado"));
    }

    @Test
    @DisplayName("Debe crear un paciente y recuperarlo por ID")
    void debeCrearPacienteYRecuperarloPorId() throws Exception {
        Medico medicoCreado = crearMedico(medico);
        Paciente pacienteCreado = crearPaciente(paciente, medicoCreado);

        this.mockMvc.perform(get("/paciente/" + pacienteCreado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pacienteCreado.getId()))
                .andExpect(jsonPath("$.nombre").value("Maria"))
                .andExpect(jsonPath("$.dni").value("888"))
                .andExpect(jsonPath("$.edad").value(20))
                .andExpect(jsonPath("$.cita").value("Ginecologia"));
    }

    @Test
    @DisplayName("Debe listar los pacientes de un médico")
    void debeListarPacientesDeUnMedico() throws Exception {
        Medico medicoCreado = crearMedico(medico);
        crearPaciente(paciente, medicoCreado);

        this.mockMvc.perform(get("/paciente/medico/" + medicoCreado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Maria"))
                .andExpect(jsonPath("$[0].dni").value("888"));
    }

    @Test
    @DisplayName("Debe actualizar un paciente correctamente")
    void debeActualizarPacienteCorrectamente() throws Exception {
        Medico medicoCreado = crearMedico(medico);
        Paciente pacienteCreado = crearPaciente(paciente, medicoCreado);

        pacienteCreado.setNombre("Maria Actualizada");
        pacienteCreado.setEdad(21);
        pacienteCreado.setCita("Revision");
        pacienteCreado.setMedico(medicoCreado);

        this.mockMvc.perform(put("/paciente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pacienteCreado)))
                .andExpect(status().isOk());

        this.mockMvc.perform(get("/paciente/" + pacienteCreado.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pacienteCreado.getId()))
                .andExpect(jsonPath("$.nombre").value("Maria Actualizada"))
                .andExpect(jsonPath("$.edad").value(21))
                .andExpect(jsonPath("$.cita").value("Revision"));
    }

    @Test
    @DisplayName("Debe eliminar un paciente correctamente")
    void debeEliminarPacienteCorrectamente() throws Exception {
        Medico medicoCreado = crearMedico(medico);
        Paciente pacienteCreado = crearPaciente(paciente, medicoCreado);

        this.mockMvc.perform(delete("/paciente/" + pacienteCreado.getId()))
                .andExpect(status().isOk());
    }
}