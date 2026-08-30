package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pb_entity_sequence")
public class PbEntityIdSequence{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // This method returns the formatted version
    public String getFormattedId() {
        return String.format("%08d", id);
    }
}
