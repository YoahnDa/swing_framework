package com.test.repository;

import com.test.entity.Etudiant;
import com.framework.repository.AbstractRepository;
import java.util.List;

public class EtudiantRepository extends AbstractRepository<Etudiant> {
    public EtudiantRepository() { super(Etudiant.class); }
    
    public List<Etudiant> trouverMajeursDuneVille(String ville) throws Exception {
        String sql = "SELECT e.* FROM etudiants e " +
                     "JOIN etablissements etab ON e.id_etab = etab.id_etab " +
                     "WHERE e.age >= 18 AND etab.ville = ?";
        return dao.findByCustomSql(entityClass,sql, ville);
    }
}
