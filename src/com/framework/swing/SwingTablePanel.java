package com.framework.swing;

import com.framework.annotation.Id;
import com.framework.annotation.JoinColumn;
import com.framework.repository.AbstractRepository;
import com.framework.repository.RepositoryRegistry;
import com.framework.utils.ReflectionUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SwingTablePanel<T> extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private AbstractRepository<T> repository;
    private Class<T> clazz;
    private Consumer<Map<String, Object>> customFilterHandler;

    private List<String> columnNames;
    private Map<Integer, JComponent> filterMap; // Relie l'index de la colonne à son composant de filtre
    private List<T> currentData;

    /**
     * @param clazz            La classe de l'entité
     * @param repository       Le repository associé
     * @param showFilters      Afficher ou non le panneau de filtres
     * @param columnsToDisplay Les colonnes à afficher (ex: ["nom", "prenom",
     *                         "etablissement"])
     * @param columnsToFilter  Les colonnes qui auront un filtre (doivent être
     *                         présentes dans l'affichage)
     */
    public SwingTablePanel(Class<T> clazz, AbstractRepository<T> repository,
            boolean showFilters,
            List<String> columnsToDisplay,
            List<String> columnsToFilter) {
        this.clazz = clazz;
        this.repository = repository;
        this.currentData = new ArrayList<>();
        this.filterMap = new HashMap<>();

        setLayout(new BorderLayout());

        // 1. DÉTERMINATION DES COLONNES
        columnNames = new ArrayList<>();
        if (columnsToDisplay != null && !columnsToDisplay.isEmpty()) {
            columnNames.addAll(columnsToDisplay);
        } else {
            for (Field f : ReflectionUtils.getMappedFields(clazz)) {
                columnNames.add(f.getName());
            }
        }

        // 2. INITIALISATION DE LA TABLE
        model = new DefaultTableModel(columnNames.toArray(new String[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // ALIGNEMENT À GAUCHE DU TEXTE
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
        }

        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        // 3. PANNEAU DES FILTRES
        if (showFilters && columnsToFilter != null && !columnsToFilter.isEmpty()) {
            // wrap 3 : Trois filtres par ligne
            JPanel filterPanel = new JPanel(
                    new MigLayout("wrap 3, fillx, insets 10", "[fill,grow][fill,grow][fill,grow]"));
            filterPanel.setBorder(BorderFactory.createTitledBorder("Filtres de recherche"));

            for (String filterCol : columnsToFilter) {
                int colIndex = columnNames.indexOf(filterCol);
                if (colIndex == -1)
                    continue; // Sécurité : la colonne filtrée doit être affichée

                // Recherche du Field pour savoir s'il y a un @JoinColumn
                Field field = null;
                try {
                    field = clazz.getDeclaredField(filterCol);
                } catch (Exception ignored) {
                }

                String labelName = filterCol.substring(0, 1).toUpperCase() + filterCol.substring(1);

                // Mini-panel pour avoir Label en haut et Input en bas
                JPanel fieldContainer = new JPanel(new MigLayout("wrap 1, insets 0", "[fill, grow]"));
                JLabel lbl = new JLabel(labelName);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                fieldContainer.add(lbl);

                JComponent input;
                if (field != null && ReflectionUtils.isJoinColumn(field)) {
                    JComboBox<Object> combo = new JComboBox<>();
                    combo.addItem("--- Tous ---"); // Option par défaut pour ne pas filtrer
                    AbstractRepository<?> targetRepo = RepositoryRegistry.getRepository(field.getType());
                    if (targetRepo != null) {
                        try {
                            targetRepo.findAll().forEach(combo::addItem);
                        } catch (Exception e) {
                        }
                    }
                    input = combo;
                } else {
                    input = new JTextField();
                }

                fieldContainer.add(input, "growx, h 30!");
                filterPanel.add(fieldContainer, "growx");
                filterMap.put(colIndex, input);
            }

            // Bouton de déclenchement du filtre (placé sur sa propre ligne avec span 3)
            JButton btnFiltrer = new JButton("Filtrer les résultats");
            btnFiltrer.putClientProperty("FlatLaf.styleClass", "default");
            btnFiltrer.addActionListener(e -> applyFilters());
            filterPanel.add(btnFiltrer, "span 3, right, wrap");

            add(filterPanel, BorderLayout.NORTH);
        }

        add(new JScrollPane(table), BorderLayout.CENTER);

        // 4. CHARGEMENT AUTOMATIQUE DES DONNÉES
        refreshData();
    }

    /**
     * NOUVEAUTÉ 2 : Permet de définir une logique de filtre côté base de données.
     */
    public void onCustomFilter(Consumer<Map<String, Object>> handler) {
        this.customFilterHandler = handler;
    }

    /**
     * NOUVEAUTÉ 3 : Modification de l'application des filtres
     */
    private void applyFilters() {
        // Si le développeur a défini un filtre personnalisé (Filtre Serveur / SQL)
        if (customFilterHandler != null) {
            Map<String, Object> filterValues = new HashMap<>();
            
            // On extrait les valeurs tapées dans l'UI
            for (Map.Entry<Integer, JComponent> entry : filterMap.entrySet()) {
                String colName = columnNames.get(entry.getKey());
                JComponent comp = entry.getValue();
                
                if (comp instanceof JTextField) {
                    String text = ((JTextField) comp).getText().trim();
                    if (!text.isEmpty()) filterValues.put(colName, text);
                } else if (comp instanceof JComboBox) {
                    Object sel = ((JComboBox<?>) comp).getSelectedItem();
                    if (sel != null && !sel.toString().equals("--- Tous ---")) {
                        filterValues.put(colName, sel);
                    }
                }
            }
            // On envoie ces valeurs au développeur pour qu'il fasse sa requête !
            customFilterHandler.accept(filterValues);
            return; // On arrête là, on ne fait pas le filtre visuel
        }

        // --- Sinon, on garde le comportement par défaut (Filtre Client Visuel) ---
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        // ... (Ton code existant avec le RowFilter.regexFilter) ...
        
        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    public T getSelectedObject() {
        int viewRow = table.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            return currentData.get(modelRow);
        }
        return null;
    }

    public JTable getTable() {
        return table;
    }

    /**
     * Comportement par défaut : Va chercher TOUS les éléments dans la base de
     * données.
     */
    public void refreshData() {
        try {
            // On récupère la liste complète via le repository
            List<T> allData = repository.findAll();
            // On délègue l'affichage à la nouvelle méthode
            setCustomData(allData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * NOUVEAU : Remplit le tableau avec une liste spécifique fournie par le
     * développeur.
     * Idéal pour les recherches complexes ou les données déjà chargées.
     */
    public void setCustomData(List<T> customData) {
        if (customData == null) {
            this.currentData = new ArrayList<>();
        } else {
            this.currentData = customData;
        }
        updateTableModel();
    }

    /**
     * Méthode interne (privée) qui dessine réellement les lignes du tableau
     * en se basant sur this.currentData.
     */
    private void updateTableModel() {
        model.setRowCount(0); // On vide le tableau visuel

        for (T obj : currentData) {
            Object[] row = new Object[columnNames.size()];
            for (int i = 0; i < columnNames.size(); i++) {
                try {
                    Object val = ReflectionUtils.getFieldValue(obj, columnNames.get(i));
                    row[i] = val != null ? val.toString() : "";
                } catch (Exception e) {
                    row[i] = "Erreur"; // Sécurité au cas où le champ n'est pas trouvé
                }
            }
            model.addRow(row);
        }
    }

    /**
     * Permet de définir une action personnalisée lors d'un double-clic sur une
     * ligne.
     * 
     * @param action Un bloc de code recevant l'objet sélectionné (ex: l'étudiant)
     */
    public void onDoubleClickHandler(Consumer<T> action) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // On vérifie s'il s'agit d'un double-clic
                if (e.getClickCount() == 2) {
                    T selected = getSelectedObject();
                    if (selected != null) {
                        action.accept(selected);
                    }
                }
            }
        });
    }
}