package com.test.entity;

import com.framework.annotation.*;

@Table(name = "etudiants")
public class Etudiant {
    @Id @Column(name = "id_etu")
    private Integer id;

    @FormField(label = "Nom de famille")
    @Required(message = "Le nom de l'étudiant est indispensable.")
    @Column(name = "nom_etu")
    private String nom;

    @FormField(label = "Prénom usuel")
    @Column(name = "prenom_etu")
    private String prenom;

    @FormField(label = "Âge de l'étudiant")
    @Column(name = "age")
    private int age;

    @FormField(label = "École d'affectation")
    @Required(message = "Un étudiant doit être assigné à un établissement.")
    @JoinColumn(name = "id_etab", eager = true) 
    private Etablissement etablissement;

    public Etudiant() {}

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

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Etablissement getEtablissement() {
        return etablissement;
    }

    public void setEtablissement(Etablissement etablissement) {
        this.etablissement = etablissement;
    }
}