package com.framework.swing;

import com.framework.annotation.*;
import com.framework.repository.AbstractRepository;
import com.framework.repository.RepositoryRegistry;
import com.framework.utils.ReflectionUtils;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;
import java.lang.reflect.Field;
import java.util.*;

public class SwingFormBuilder {

    // --- LOGIQUE DE MANIPULATION DES DONNÉES ---

    /** Vider tous les champs du formulaire */
    public static void clearForm(Map<String, JComponent> fields) {
        fields.forEach((name, comp) -> {
            if (comp instanceof JTextField)
                ((JTextField) comp).setText("");
            else if (comp instanceof JSpinner)
                ((JSpinner) comp).setValue(0);
            else if (comp instanceof JComboBox)
                ((JComboBox<?>) comp).setSelectedIndex(0);
        });
    }

    /** Remplir le formulaire à partir d'un objet existant (pour l'Update) */
    public static void fillFormFromObject(Map<String, JComponent> fields, Object object) {
        fields.forEach((name, comp) -> {
            try {
                Object value = ReflectionUtils.getFieldValue(object, name);
                if (comp instanceof JTextField)
                    ((JTextField) comp).setText(value != null ? value.toString() : "");
                else if (comp instanceof JSpinner)
                    ((JSpinner) comp).setValue(value != null ? value : 0);
                else if (comp instanceof JComboBox)
                    ((JComboBox<Object>) comp).setSelectedItem(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /** Extraire les données du formulaire vers un objet Java */
    public static <T> void fillObjectFromForm(Map<String, JComponent> fields, T object) throws Exception {
        for (Map.Entry<String, JComponent> entry : fields.entrySet()) {
            JComponent comp = entry.getValue();
            Object value = null;
            if (comp instanceof JTextField)
                value = ((JTextField) comp).getText();
            else if (comp instanceof JSpinner)
                value = ((JSpinner) comp).getValue();
            else if (comp instanceof JComboBox)
                value = ((JComboBox<?>) comp).getSelectedItem();

            ReflectionUtils.setFieldValue(object, entry.getKey(), value);
        }
    }

    // --- LE GÉNÉRATEUR DE FORMULAIRE ---

    /**
     * @param showSave   Active le bouton Sauvegarder
     * @param showUpdate Active le bouton Modifier
     * @param showDelete Active le bouton Supprimer
     */
    public static <T> Map<String, JComponent> buildForm(
            JPanel panel, Class<T> clazz, AbstractRepository<T> repo,
            boolean showSave, boolean showUpdate, boolean showDelete,
            Map<String, Runnable> customButtons, T existingObject, Runnable onSuccess) {

        // CHANGEMENT 1 : wrap 3 (3 éléments par ligne) et 3 colonnes de taille égale
        panel.setLayout(new MigLayout("wrap 3, fillx, insets 20", "[fill,grow][fill,grow][fill,grow]"));
        Map<String, JComponent> fieldsMap = new HashMap<>();

        // Construction des champs
        for (Field field : ReflectionUtils.getMappedFields(clazz)) {
            if (field.isAnnotationPresent(Id.class) && ReflectionUtils.isIdAutoIncrement(field))
                continue;

            // CHANGEMENT 2 : Détermination du nom du label via @FormField
            String labelName = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
            if (field.isAnnotationPresent(FormField.class)) {
                labelName = field.getAnnotation(FormField.class).label();
            }

            // CHANGEMENT 3 : Création d'un mini-panel pour empiler Label au-dessus de Input
            JPanel fieldContainer = new JPanel(new MigLayout("wrap 1, insets 0", "[fill, grow]"));

            // On ajoute le label en gras pour plus de lisibilité
            JLabel lbl = new JLabel(labelName);
            lbl.setFont(lbl.getFont().deriveFont(java.awt.Font.BOLD));
            fieldContainer.add(lbl);

            JComponent input;
            if (ReflectionUtils.isJoinColumn(field)) {
                JComboBox<Object> combo = new JComboBox<>();
                AbstractRepository<?> targetRepo = RepositoryRegistry.getRepository(field.getType());
                if (targetRepo != null) {
                    try {
                        targetRepo.findAll().forEach(combo::addItem);
                    } catch (Exception e) {
                    }
                }
                input = combo;
            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                input = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
            } else {
                input = new JTextField();
            }

            // On ajoute l'input sous le label
            fieldContainer.add(input, "growx, h 30!"); // h 30! force une belle hauteur pour l'input
            fieldsMap.put(field.getName(), input);

            // On ajoute le bloc complet au panel principal
            panel.add(fieldContainer, "growx");
        }

        // Pré-remplissage si un objet existe
        if (existingObject != null) {
            fillFormFromObject(fieldsMap, existingObject);
        }

        // Barre de boutons
        JPanel btnBar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton btnReset = new JButton("Réinitialiser");
        btnReset.addActionListener(e -> clearForm(fieldsMap));
        btnBar.add(btnReset);

        if (customButtons != null) {
            customButtons.forEach((name, action) -> {
                JButton b = new JButton(name);
                b.addActionListener(e -> action.run());
                btnBar.add(b);
            });
        }

        if (showSave) {
            JButton bSave = new JButton("Enregistrer");
            bSave.putClientProperty("FlatLaf.styleClass", "default");
            bSave.addActionListener(e -> {
                List<String> errors = validateForm(fieldsMap, clazz);
                if (!errors.isEmpty()) {
                    JOptionPane.showMessageDialog(panel,
                            "Veuillez corriger les erreurs :\n" + String.join("\n", errors), "Erreur",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    T obj = clazz.getDeclaredConstructor().newInstance();
                    fillObjectFromForm(fieldsMap, obj);
                    repo.save(obj);
                    JOptionPane.showMessageDialog(panel, "Créé avec succès !");
                    if (onSuccess != null) {
                        onSuccess.run(); 
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            btnBar.add(bSave);
        }

        if (showUpdate && existingObject != null) {
            JButton bUpd = new JButton("Modifier");
            bUpd.addActionListener(e -> {
                List<String> errors = validateForm(fieldsMap, clazz);
                if (!errors.isEmpty()) {
                    JOptionPane.showMessageDialog(panel,
                            "Veuillez corriger les erreurs :\n" + String.join("\n", errors), "Erreur",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    fillObjectFromForm(fieldsMap, existingObject);
                    repo.save(existingObject);
                    JOptionPane.showMessageDialog(panel, "Modifié avec succès !");

                    if (onSuccess != null) {
                        onSuccess.run(); 
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            btnBar.add(bUpd);
        }

        if (showDelete && existingObject != null) {
            JButton bDel = new JButton("Supprimer");
            bDel.putClientProperty("FlatLaf.styleClass", "danger"); // Style rouge si configuré
            bDel.addActionListener(e -> {
                try {
                    if (JOptionPane.showConfirmDialog(panel, "Supprimer ?") == JOptionPane.YES_OPTION) {
                        repo.delete(existingObject);
                        clearForm(fieldsMap);
                        JOptionPane.showMessageDialog(panel, "Supprimé !");
                        if (onSuccess != null) {
                            onSuccess.run(); 
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            btnBar.add(bDel);
        }

        // CHANGEMENT 4 : La barre de bouton doit s'étendre sur les 3 colonnes ("span
        // 3")
        panel.add(btnBar, "span 3, right, wrap");

        return fieldsMap;
    }

    /**
     * Valide les données du formulaire avant l'extraction.
     * Retourne une liste de messages d'erreur (vide si tout est correct).
     */
    public static List<String> validateForm(Map<String, JComponent> fields, Class<?> clazz) {
        List<String> errors = new ArrayList<>();

        for (Field field : ReflectionUtils.getMappedFields(clazz)) {
            JComponent comp = fields.get(field.getName());

            // On ignore les champs qui ne sont pas dans le formulaire (ex: ID
            // auto-incrémenté)
            if (comp == null)
                continue;

            // 1. Validation : Champ Obligatoire (@Required)
            if (field.isAnnotationPresent(Required.class)) {
                Required reqAnnotation = field.getAnnotation(Required.class);
                boolean isEmpty = false;

                if (comp instanceof JTextField) {
                    isEmpty = ((JTextField) comp).getText().trim().isEmpty();
                } else if (comp instanceof JComboBox) {
                    isEmpty = ((JComboBox<?>) comp).getSelectedItem() == null;
                }
                // (Un JSpinner a toujours une valeur par défaut, donc il n'est jamais vraiment
                // "vide")

                if (isEmpty) {
                    // On formate le nom du champ pour un affichage propre (ex: "nom_etu" ->
                    // "Nom_etu")
                    String niceName = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
                    errors.add("- " + niceName + " : " + reqAnnotation.message());
                }
            }
        }

        return errors;
    }
}