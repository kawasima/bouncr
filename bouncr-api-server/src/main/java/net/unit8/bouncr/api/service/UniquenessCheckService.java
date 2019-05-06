package net.unit8.bouncr.api.service;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class UniquenessCheckService<E> {
    private EntityManager em;

    public UniquenessCheckService(EntityManager em) {
        this.em = em;
    }

    public boolean isUnique(Class<E> entityClass, String uniqueAttrName, Object value) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        query.select(cb.count(cb.literal(1)));
        Root<E> root = query.from(entityClass);
        query.where(cb.equal(root.get(uniqueAttrName), value));
        Long cnt = em.createQuery(query).getSingleResult();
        return cnt == 0;
    }
}
