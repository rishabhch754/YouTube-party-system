package com.watchparty.repository;

import com.watchparty.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, String> {
    Optional<RoomEntity> findByRoomId(String roomId);
    boolean existsByRoomId(String roomId);
    void deleteByRoomId(String roomId);
}