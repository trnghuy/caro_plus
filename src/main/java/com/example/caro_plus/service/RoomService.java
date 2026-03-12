package com.example.caro_plus.service;

import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    // tạo phòng
    public Room createRoom(User user) {

        Room room = new Room();
        room.setHost(user);
        room.setStatus("waiting");

        return roomRepository.save(room);
    }

    // join phòng
    public Room joinRoom(Long roomId) {

        Optional<Room> roomOptional = roomRepository.findById(roomId);

        if(roomOptional.isPresent()){

            Room room = roomOptional.get();

            if(room.getStatus().equals("waiting")){
                room.setStatus("full");
            }

            return roomRepository.save(room);
        }

        return null;
    }

    // leave phòng
    public void leaveRoom(Long roomId){

        Optional<Room> roomOptional = roomRepository.findById(roomId);

        if(roomOptional.isPresent()){
            Room room = roomOptional.get();
            room.setStatus("waiting");
            roomRepository.save(room);
        }

    }

    // start game
    public Room startGame(Long roomId){

        Optional<Room> roomOptional = roomRepository.findById(roomId);

        if(roomOptional.isPresent()){
            Room room = roomOptional.get();
            room.setStatus("playing");
            return roomRepository.save(room);
        }

        return null;
    }

}
