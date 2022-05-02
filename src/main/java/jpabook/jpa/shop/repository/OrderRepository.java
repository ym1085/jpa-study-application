package jpabook.jpa.shop.repository;

import jpabook.jpa.shop.domain.Order;
import jpabook.jpa.shop.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private Logger log = LoggerFactory.getLogger(OrderRepository.class);

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findById(Long id) {
        return em.find(Order.class, id);
    }

    public List<Order> findAllByString(OrderSearch orderSearch) {

        String jpql = "select o from Order o join o.member m";
        boolean isFirstCondition = true;

        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }

        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000);

        log.info("orderSearch.getOrderStatus = {}", orderSearch.getOrderStatus());
        if (orderSearch.getOrderStatus() != null && orderSearch.getOrderStatus() != "") {
            log.error("Error!");
            OrderStatus orderStatus = OrderStatus.valueOf(orderSearch.getOrderStatus()); // 220428(목) 기존 enum String 타입으로 그냥 넣었을 때 계속 Error 발생 -> 변경
            log.info("orderStatus = {}", orderStatus);

            query = query.setParameter("status", orderStatus);
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        log.info("query = {}", query);
        return query.getResultList();
    }

    /**
     * JPA Criteria
     */
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Object, Object> m = o.join("member", JoinType.INNER);

        List<Predicate> criteria = new ArrayList<>();

        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());
            criteria.add(status);
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" + orderSearch.getMemberName() + "%");
            criteria.add(name);
        }

        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000);
        return query.getResultList();
    }

    // join fetch
    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class
        ).getResultList();
    }

    public List<Order> findAllWithItem() {
        // 1 : N fetch join에서는 페이징 처리를 하면 안된다
        // 1 : N fetch join에서의 페이징 처리는 상당히 위험한 행위, 메모리에서 페이징
        // 컬렉션 fetch join은 반드시 1개만 써야 한다. 한 개도 데이터 뻥튀기 되는데 2개면 답이 없음

        return em.createQuery(
//                "select o from Order o" + // -> ID가 동일하면 Distinct 쳐서 반환 해준다(중복 제거 하고)
                        "select distinct o from Order o" +
                        " join fetch o.member m" + // N : 1
                        " join fetch o.delivery d" + // 1 : 1
                        " join fetch o.orderItems oi" + // 1 : N -> data 뻥튀기 발생!
                        " join fetch oi.item i", Order.class)
                .setFirstResult(1)
                .setMaxResults(100)
                .getResultList();
    }
}
