package org.example.ecommerce.users.repository.pagination;

import org.example.ecommerce.users.dto.request.UserCursorPayload;
import org.example.ecommerce.users.entity.User;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface UserRepositoryCustom {

    List<User> findPageWithCards(Specification<User> specification,
                                 int size,
                                 SortDirection direction,
                                 UserCursorPayload cursor);

}
