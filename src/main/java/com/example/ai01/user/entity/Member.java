package com.example.ai01.user.entity;

import com.example.ai01.global.entity.BaseEntity;
import com.example.ai01.user.dto.request.MemberRequest;
import jakarta.persistence.*;
import lombok.*;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;



@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Member extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String username;

    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String roles;  // 예: "ROLE_USER,ROLE_ADMIN"


    public static Member DTOtoEntity(MemberRequest.CreateMember dto){
        Member member = new Member();
        member.name = dto.getName();
        member.password = dto.getPassword();
        member.username = dto.getUsername();
        member.nickname = dto.getNickname();
        member.phone = dto.getPhone();
        member.email = dto.getEmail();

        return member;
    }



}
