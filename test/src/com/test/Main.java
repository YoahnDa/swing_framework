package com.test;

import com.formdev.flatlaf.FlatDarkLaf;
import com.framework.repository.RepositoryRegistry;
import com.framework.swing.SwingFormBuilder;
import com.framework.swing.SwingTablePanel;
import com.test.entity.Etablissement;
import com.test.entity.Etudiant;
import com.test.repository.EtablissementRepository;
import com.test.repository.EtudiantRepository;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main extends JFrame {

    private EtudiantRepository etuRepo = new EtudiantRepository();
    private EtablissementRepository etabRepo = new EtablissementRepository();
    
    private JPanel formContainer; 
    private SwingTablePanel<Etudiant> tablePanel;

    public Main() {
        // 1. Initialisation Technique
        RepositoryRegistry.register(Etablissement.class, etabRepo);
        RepositoryRegistry.register(Etudiant.class, etuRepo);

        setTitle("Gestion des Étudiants - Framework Testing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // --- PARTIE HAUTE : Le Formulaire ---
        formContainer = new JPanel(new BorderLayout());
        formContainer.setBorder(BorderFactory.createTitledBorder("Détails de l'Étudiant"));
        add(formContainer, BorderLayout.NORTH);

        // --- PARTIE CENTRALE : La Liste ---
        List<String> colonnes = List.of("nom", "prenom", "age", "etablissement");
        tablePanel = new SwingTablePanel<>(Etudiant.class, etuRepo, true, colonnes, colonnes);
        add(tablePanel, BorderLayout.CENTER);

        // 3. LOGIQUE D'INTERACTION
        tablePanel.getTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Etudiant selection = tablePanel.getSelectedObject();
                if (selection != null) {
                    chargerFormulaire(selection);
                }
            }
        });

        // 4. CHARGEMENT INITIAL
        chargerFormulaire(null);
    }

    private void chargerFormulaire(Etudiant etu) {
        formContainer.removeAll();
        
        boolean isUpdate = (etu != null);
        
        // Actions personnalisées
        Map<String, Runnable> actions = new LinkedHashMap<>();
        
        // Bouton "Nouveau" : On vide le formulaire ET on désélectionne la table
        actions.put("Nouveau / Ajouter", () -> {
            tablePanel.getTable().clearSelection(); // CORRECTION ICI
            chargerFormulaire(null);
        });

        // Génération du formulaire avec le Callback de succès
        SwingFormBuilder.buildForm(
            formContainer, 
            Etudiant.class, 
            etuRepo, 
            !isUpdate, 
            isUpdate,  
            isUpdate,  
            actions, 
            etu,
            // --- CORRECTION : LE CALLBACK ON_SUCCESS ---
            () -> {
                tablePanel.refreshData(); // 1. Met à jour les données de la base
                chargerFormulaire(null);  // 2. Remet le formulaire en mode "Ajout" propre
            }
        );

        // Rafraîchir l'affichage
        formContainer.revalidate();
        formContainer.repaint();
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}