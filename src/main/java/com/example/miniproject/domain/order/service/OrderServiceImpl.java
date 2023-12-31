package com.example.miniproject.domain.order.service;

import com.example.miniproject.domain.cart.entity.CartItem;
import com.example.miniproject.domain.cart.repository.CartRepository;
import com.example.miniproject.domain.member.entity.Member;
import com.example.miniproject.domain.member.repository.MemberRepository;
import com.example.miniproject.domain.order.dto.request.OrderItemRegisterRequest;
import com.example.miniproject.domain.order.dto.request.OrderRegisterRequest;
import com.example.miniproject.domain.order.dto.response.OrderRegisterResponse;
import com.example.miniproject.domain.order.dto.response.OrderResponse;
import com.example.miniproject.domain.order.entity.Order;
import com.example.miniproject.domain.order.entity.OrderItem;
import com.example.miniproject.domain.order.repository.OrderItemRepository;
import com.example.miniproject.domain.order.repository.OrderRepository;
import com.example.miniproject.domain.roomtype.entity.Room;
import com.example.miniproject.domain.roomtype.entity.RoomType;
import com.example.miniproject.domain.roomtype.repository.RoomTypeRepository;
import com.example.miniproject.domain.roomtype.service.RoomTypeService;
import com.example.miniproject.global.exception.AccessForbiddenException;
import com.example.miniproject.global.exception.NoStockException;
import com.example.miniproject.global.exception.NoSuchEntityException;
import com.example.miniproject.global.utils.CodeGenerator;
import com.example.miniproject.global.utils.PriceCalculator;
import com.example.miniproject.global.utils.ScheduleValidator;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final MemberRepository memberRepository;
    private final RoomTypeService roomTypeService;
    private final CartRepository cartRepository;

    @Override
    @Transactional
    public OrderRegisterResponse registerOrder(Long memberId, OrderRegisterRequest request) {
        Member member = memberRepository.getReferenceById(memberId);
        Order order = orderRepository.save(
            request.toEntity(member, CodeGenerator.generate())
        );
        request.orderItems()
            .forEach(orderItemRegisterRequest -> {
                ScheduleValidator.validate(
                    orderItemRegisterRequest.checkinDate(),
                    orderItemRegisterRequest.checkoutDate()
                );
                registerOrderItem(order, orderItemRegisterRequest);
                deleteOneCartItem(member, orderItemRegisterRequest);
            });
        return new OrderRegisterResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse getOrder(Long memberId, Long orderId) {
        Member member = memberRepository.getReferenceById(memberId);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(NoSuchEntityException::new);
        if (!Objects.equals(member, order.getMember())) {
            throw new AccessForbiddenException();
        }
        return new OrderResponse(order);
    }

    private void registerOrderItem(Order order, OrderItemRegisterRequest request) {
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
            .orElseThrow(NoSuchEntityException::new);
        Room room = findAvailableRoom(
            roomType,
            request.checkinDate(),
            request.checkoutDate()
        );
        int price = PriceCalculator.calculateRoomTypePrice(
            roomType,
            request.checkinDate(),
            request.checkoutDate()
        );
        OrderItem orderItem = orderItemRepository.save(
            request.toEntity(
                order,
                room,
                CodeGenerator.generate(),
                price
            )
        );
        order.addOrderItem(orderItem);
    }

    private Room findAvailableRoom(
        RoomType roomType,
        LocalDate checkinDate,
        LocalDate checkoutDate
    ) {
        return roomTypeService.findAvailableRoom(
            roomType,
            checkinDate,
            checkoutDate
        ).orElseThrow(NoStockException::new);
    }

    private void deleteOneCartItem(Member member, OrderItemRegisterRequest request) {
        List<CartItem> cartItems = cartRepository.findByMemberAndRoomTypeIdAndCheckinDateAndCheckoutDate(
            member,
            request.roomTypeId(),
            request.checkinDate(),
            request.checkoutDate()
        );
        if (!cartItems.isEmpty()) {
            cartRepository.deleteById(cartItems.get(0).getId());
        }
    }
}
