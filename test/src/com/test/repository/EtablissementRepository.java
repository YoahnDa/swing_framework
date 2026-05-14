package com.test.repository;

import com.test.entity.Etablissement;
import com.framework.repository.AbstractRepository;
import java.util.List;

public class EtablissementRepository extends AbstractRepository<Etablissement> {
    public EtablissementRepository() { super(Etablissement.class); }
}
