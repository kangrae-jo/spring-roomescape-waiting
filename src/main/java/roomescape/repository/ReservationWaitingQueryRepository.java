package roomescape.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import roomescape.domain.ReservationWaiting;
import roomescape.domain.projection.ReservationWaitingWithOrder;

public interface ReservationWaitingQueryRepository extends JpaRepository<ReservationWaiting, Long> {

    @NativeQuery(
            value = """
                    WITH target_waiting AS (
                        SELECT id, date, time_id, theme_id
                        FROM reservation_waiting
                        WHERE id = :id
                    ),
                    ordered_waiting AS (
                        SELECT
                            rw.id as waiting_id,
                            rw.name as waiting_name,
                            rw.date,
                            rw.time_id,
                            rw.theme_id,
                            ROW_NUMBER() OVER (
                                PARTITION BY rw.date, rw.time_id, rw.theme_id
                                ORDER BY rw.id ASC
                            ) as waiting_order
                        FROM reservation_waiting rw
                        INNER JOIN target_waiting tw
                            ON rw.date = tw.date
                            AND rw.time_id = tw.time_id
                            AND rw.theme_id = tw.theme_id
                    )
                    SELECT
                        ow.waiting_id, ow.waiting_name, ow.waiting_order, ow.date,
                        t.id as time_id, t.start_at as time_value,
                        th.id as theme_id, th.name as theme_name,
                        th.description as theme_description, th.thumbnail_image_url as theme_thumbnail
                    FROM ordered_waiting ow
                    INNER JOIN reservation_time t ON ow.time_id = t.id
                    INNER JOIN theme th ON ow.theme_id = th.id
                    INNER JOIN target_waiting tw ON ow.waiting_id = tw.id
                    """,
            sqlResultSetMapping = "ReservationWaitingWithOrderMapping"
    )
    Optional<ReservationWaitingWithOrder> findWithOrderById(@Param("id") Long id);

    @NativeQuery(
            value = """
                    WITH my_waiting AS (
                        SELECT id, date, time_id, theme_id
                        FROM reservation_waiting
                        WHERE name = :name
                    ),
                    ordered_waiting AS (
                        SELECT
                            rw.id as waiting_id,
                            rw.name as waiting_name,
                            rw.date,
                            rw.time_id,
                            rw.theme_id,
                            ROW_NUMBER() OVER (
                                PARTITION BY rw.date, rw.time_id, rw.theme_id
                                ORDER BY rw.id ASC
                            ) as waiting_order
                        FROM reservation_waiting rw
                        INNER JOIN my_waiting mw
                            ON rw.date = mw.date
                            AND rw.time_id = mw.time_id
                            AND rw.theme_id = mw.theme_id
                    )
                    SELECT
                        ow.waiting_id, ow.waiting_name, ow.waiting_order, ow.date,
                        t.id as time_id, t.start_at as time_value,
                        th.id as theme_id, th.name as theme_name,
                        th.description as theme_description, th.thumbnail_image_url as theme_thumbnail
                    FROM ordered_waiting ow
                    INNER JOIN reservation_time t ON ow.time_id = t.id
                    INNER JOIN theme th ON ow.theme_id = th.id
                    INNER JOIN my_waiting mw ON ow.waiting_id = mw.id
                    ORDER BY ow.date DESC, t.start_at ASC, ow.waiting_order ASC
                    """,
            sqlResultSetMapping = "ReservationWaitingWithOrderMapping"
    )
    List<ReservationWaitingWithOrder> findByName(@Param("name") String name);
}
