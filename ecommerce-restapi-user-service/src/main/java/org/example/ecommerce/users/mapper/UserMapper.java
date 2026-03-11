package org.example.ecommerce.users.mapper;

import org.example.ecommerce.users.dto.request.UserRequest;
import org.example.ecommerce.users.dto.request.UserUpdateRequest;
import org.example.ecommerce.users.dto.response.UserListResponse;
import org.example.ecommerce.users.dto.response.UserResponse;
import org.example.ecommerce.users.entity.User;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = PaymentCardMapper.class, builder = @Builder(disableBuilder = true))
public interface UserMapper {

    @Mapping(target = "paymentCards", ignore = true)
    User toEntity(UserRequest request);

    @Mapping(target = "paymentCards", ignore = true)
    void updateEntity(UserUpdateRequest request, @MappingTarget User entity);

    UserResponse toResponse(User entity);

    UserListResponse toListResponse(User user);

}
