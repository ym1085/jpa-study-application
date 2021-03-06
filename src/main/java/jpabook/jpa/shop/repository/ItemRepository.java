package jpabook.jpa.shop.repository;

import jpabook.jpa.shop.domain.item.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemRepository {

    private final EntityManager em;

    public void save(Item item) {
        if (item.getId() == null) {
            em.persist(item);
        } else {
            em.merge(item); // id 값이 존재하면, 해당 라인이 실행 됨
        }
    }

    public List<Item> findAll() {
        return em.createQuery("select i from Item i", Item.class)
                 .getResultList();
    }

    public Item findById(Long id) {
        return em.find(Item.class, id);
    }
}
