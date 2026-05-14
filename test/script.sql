CREATE TABLE etablissements (
    id_etab SERIAL PRIMARY KEY,
    nom_etab VARCHAR(100) NOT NULL,
    ville VARCHAR(50)
);


CREATE TABLE etudiants (
    id_etu SERIAL PRIMARY KEY,
    nom_etu VARCHAR(50) NOT NULL,
    prenom_etu VARCHAR(50),
    age INTEGER,
    id_etab INTEGER,
    CONSTRAINT fk_etablissement FOREIGN KEY (id_etab) REFERENCES etablissements(id_etab)
);