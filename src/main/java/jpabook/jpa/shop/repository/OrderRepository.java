package jpabook.jpa.shop.repository;

import jpabook.jpa.shop.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findById(Long id) {
        return em.find(Order.class, id);
    }

    // 검색
    public List<Order> findAllByString(OrderSearch orderSearch) {
        // 기본적으로 값이 모두 있다 가정하고 JPQL을 사용하는 방식
        /*return em.createQuery("select o from Order o join o.member m" +
                                            " where o.status = :status" +
                                            " and m.username like :name", Order.class)
                 .setParameter("status", orderSearch.getOrderStatus())
                 .setParameter("name", orderSearch.getMemberName())
                 .setFirstResult(100)
                 .setMaxResults(1000) // 최대 1000건
                 .getResultList();*/

        // JPQL 동적 쿼리 작성
        String jpql = "select o from Order o join o.member m";
        boolean isFirstCondition = true;

        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }

        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like = :name";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                                              .setMaxResults(1000);

        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }

        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        return query.getResultList();
    }

    // JPA 표준 동적 쿼리, QueryDSL 사용하는게 더 좋음
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        // 권장 방식이 아님
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Object, Object> member = o.join("member", JoinType.INNER);

        List<Predicate> criteria = new ArrayList<>();

        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());
            criteria.add(status);
        }

        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name = cb.like(member.get("name"), "%" + orderSearch.getMemberName() + "%");
            criteria.add(name);
        }

        // 조건절
        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000);
        return query.getResultList();
    }
}
