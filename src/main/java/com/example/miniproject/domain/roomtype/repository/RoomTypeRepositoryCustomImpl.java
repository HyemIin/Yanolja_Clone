package com.example.miniproject.domain.roomtype.repository;

import static com.example.miniproject.domain.order.entity.QOrderItem.orderItem;
import static com.example.miniproject.domain.room.entity.QRoom.room;

import com.example.miniproject.domain.room.entity.QRoom;
import com.example.miniproject.domain.roomtype.entity.RoomType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoomTypeRepositoryCustomImpl implements RoomTypeRepositoryCustom {

    private final JPAQueryFactory query;

    @Override
    public Long findStockBySchedule(
        RoomType roomType,
        LocalDate checkinDate,
        LocalDate checkoutDate
    ) {
        return query
            .select(room.count())
            .from(room)
            .where(
                room.roomType.eq(roomType),
                validRooms(checkinDate, checkoutDate)
            )
            .fetchOne();
    }

    private BooleanExpression validRooms(LocalDate checkinDate, LocalDate checkoutDate) {
        QRoom subRoom = new QRoom("subRoom");
        return room
            .notIn(
                JPAExpressions
                    .selectFrom(subRoom)
                    .join(orderItem).on(orderItem.room.eq(subRoom))
                    .where(
                        overlapped(checkinDate, checkoutDate)
                    )
            );
    }

    private BooleanExpression overlapped(LocalDate checkinDate, LocalDate checkoutDate) {
        return orderItem.checkinDate.between(checkinDate, checkoutDate.minusDays(1)).and(
            orderItem.checkoutDate.between(checkinDate.plusDays(1), checkoutDate));
    }
}
