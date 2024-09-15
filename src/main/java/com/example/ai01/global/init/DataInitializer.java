package com.example.ai01.global.init;
import com.example.ai01.user.dto.request.MemberRequest;
import com.example.ai01.user.entity.Member;
import com.example.ai01.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import net.andreinc.mockneat.MockNeat;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DataInitializer implements CommandLineRunner {


    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) throws Exception {
        // Check if the members already exist in the database
        long count = memberRepository.count();
        if (count == 0) {
            MockNeat mock = MockNeat.threadLocal();

            for (int i = 1; i <= 1000; i++) {
                String name = mock.names().full().val();
                String nickname = mock.words().val() + mock.ints().range(1, 100).val();
                String email = mock.emails().val();
                String phone = mock.regex("\\d{3}-\\d{3}-\\d{4}").val();
                String username = mock.words().val() + i;
                String password = passwordEncoder.encode("password" + i); // 생성되는 비밀번호 형식은

                MemberRequest.CreateMember createMember = new MemberRequest.CreateMember();
                createMember.setName(name);
                createMember.setNickname(nickname);
                createMember.setEmail(email);
                createMember.setPhone(phone);
                createMember.setUsername(username);
                createMember.setPassword(password);

                Member member = Member.DTOtoEntity(createMember);
                memberRepository.save(member);
            }

            System.out.println("Dummy data created successfully.");
        } else {
            System.out.println("Dummy data already exists, skipping data creation.");
        }

    }
}
