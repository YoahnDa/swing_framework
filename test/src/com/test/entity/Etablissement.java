package com.test.entity;

import com.framework.annotation.*;

@Table(name = "etablissements")
public class Etablissement {
    @Id
    @Column(name = "id_etab")
    private Integer id;

    @Column(name = "nom_etab")
    private String nom;

    @Column(name = "ville")
    private String ville;

    public Etablissement() {
    }

    public Etablissement(String nom, String ville) {
        this.nom = nom;
        this.ville = ville;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    @Override
    public String toString() {
        return this.nom; 
    }
}