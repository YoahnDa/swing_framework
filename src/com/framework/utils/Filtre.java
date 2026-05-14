package com.framework.utils;

public class Filtre {
    private String attributJava; // nom de la variable dans la classe (ex: "nom", "prix", "categorie")
    private String operateur;    // "=", ">", "<", "LIKE", ">=", etc.
    private Object valeur;       // La valeur à chercher

    public Filtre(String attributJava, String operateur, Object valeur) {
        this.attributJava = attributJava;
        this.operateur = operateur;
        this.valeur = valeur;
    }

    public String getAttributJava() { return attributJava; }
    public String getOperateur() { return operateur; }
    public Object getValeur() { return valeur; }
}