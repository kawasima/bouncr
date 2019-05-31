package net.unit8.bouncr.api.resource;

import javax.persistence.*;
import javax.persistence.criteria.*;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MockFactory {
    public static EntityManager createEntityManagerMock(Object... mocks) {
        List<Object> mockList = Arrays.asList(mocks);
        EntityManager em = mock(EntityManager.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        CriteriaQuery query = mockList.stream()
                .filter(CriteriaQuery.class::isInstance)
                .map(CriteriaQuery.class::cast)
                .findAny()
                .orElse(mock(CriteriaQuery.class));
        Root<?> root = mockList.stream()
                .filter(Root.class::isInstance)
                .map(Root.class::cast)
                .findAny()
                .orElse(mock(Root.class));
        Path path = mock(Path.class);
        Join join = mock(Join.class);
        EntityGraph graph = mockList.stream()
                .filter(EntityGraph.class::isInstance)
                .map(EntityGraph.class::cast)
                .findAny()
                .orElse(mock(EntityGraph.class));
        Subgraph subgraph = mock(Subgraph.class);

        TypedQuery typedQuery = mockList.stream()
                .filter(TypedQuery.class::isInstance)
                .map(TypedQuery.class::cast)
                .findAny()
                .orElse(mock(TypedQuery.class));

        EntityTransaction tx = mockList.stream()
                .filter(EntityTransaction.class::isInstance)
                .map(EntityTransaction.class::cast)
                .findAny()
                .orElse(mock(EntityTransaction.class));

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(any(Class.class))).thenReturn(query);
        when(query.from(any(Class.class))).thenReturn(root);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(em.createEntityGraph(any(Class.class))).thenReturn(graph);
        when(root.join(anyString())).thenReturn(join);
        when(root.get(anyString())).thenReturn(path);
        when(join.join(anyString())).thenReturn(join);
        when(graph.addSubgraph(anyString())).thenReturn(subgraph);
        when(subgraph.addSubgraph(anyString())).thenReturn(subgraph);
        when(em.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);
        when(typedQuery.setHint(anyString(), any())).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        when(em.getTransaction()).thenReturn(tx);
        return em;
    }


}
