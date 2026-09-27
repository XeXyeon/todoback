# Board REST API

Spring Boot 3.5.16, Java 21, Spring Data JPA, Spring Security, H2로 만든 게시판 API입니다.

## 실행

JDK 21과 OpenSSL이 필요합니다. 별도 DB 설치는 필요하지 않습니다. 저장소 루트에서 다음 한 명령으로 실행합니다.

```bash
JWT_SECRET="$(openssl rand -hex 32)" ./gradlew bootRun
```

서버 주소는 `http://localhost:8081`입니다. H2 데이터는 실행 디렉터리의 `boarddb.mv.db`에 저장됩니다. 포트를 바꾸려면 `SERVER_PORT=8082`를 명령 앞에 추가합니다. 매번 새 JWT 키를 생성하므로 서버를 재시작하면 기존 토큰은 무효가 됩니다. 배포할 때는 별도로 관리하는 32바이트 이상의 `JWT_SECRET`을 사용하세요. 키를 저장소에 넣지 않습니다.

테스트: `./gradlew test` (테스트는 자체 H2 DB와 테스트용 키를 사용합니다).

## API 명세

JSON 요청에는 `Content-Type: application/json`을 사용합니다. 인증이 필요한 요청에는 `Authorization: Bearer <accessToken>`을 보냅니다. 날짜와 시각은 ISO-8601 문자열입니다. `PUT`은 전달한 리소스의 수정 가능한 필드를 모두 교체합니다.

| 메서드 | 주소 | 인증 | 요청 본문 | 성공 응답 |
| --- | --- | --- | --- | --- |
| POST | `/members/signup` | 없음 | `{"email":"a@example.com","password":"password123","nickname":"Kim"}` | 201, 회원 DTO |
| POST | `/auth/login` | 없음 | `{"email":"a@example.com","password":"password123"}` | 200, `{"accessToken":"...","tokenType":"Bearer"}` |
| GET | `/me` | 필요 | 없음 | 200, 회원 DTO |
| POST | `/posts` | 필요 | `{"title":"제목","content":"본문"}` | 201, 게시글 DTO |
| GET | `/posts?page=0&size=20` | 없음 | 없음 | 200, 페이지 DTO |
| GET | `/posts/{id}` | 없음 | 없음 | 200, 게시글 DTO |
| PUT | `/posts/{id}` | 필요 | `{"title":"새 제목","content":"새 본문"}` | 200, 게시글 DTO |
| DELETE | `/posts/{id}` | 필요 | 없음 | 204, 본문 없음 |
| POST | `/posts/{postId}/comments` | 필요 | `{"content":"댓글"}` | 201, 댓글 DTO |
| GET | `/posts/{postId}/comments` | 없음 | 없음 | 200, 댓글 DTO 배열 |
| PUT | `/posts/{postId}/comments/{id}` | 필요 | `{"content":"새 댓글"}` | 200, 댓글 DTO |
| DELETE | `/posts/{postId}/comments/{id}` | 필요 | 없음 | 204, 본문 없음 |
| GET | `/health` | 없음 | 없음 | 200, `OK` |

회원 DTO는 `id`, `email`, `nickname`, `createdAt`입니다. 게시글 DTO는 `id`, `title`, `content`, `authorNickname`, `createdAt`, `updatedAt`입니다. 댓글 DTO는 `id`, `postId`, `content`, `authorNickname`, `createdAt`, `updatedAt`입니다. 목록 응답의 `content` 배열에는 글마다 `id`, `title`, `authorNickname`, `commentCount`, `createdAt`, `updatedAt`이 들어갑니다. 페이지 응답에는 `page`, `size`, `totalElements`, `totalPages`가 들어갑니다. 페이지 번호는 0부터 시작하고 크기는 1~100입니다.

이메일은 이메일 형식이어야 하고, 비밀번호는 8자 이상, 닉네임은 1~50자입니다. 글 제목은 1~100자, 본문은 1~5000자, 댓글은 1~2000자입니다. 공백만 있는 값은 허용하지 않습니다.

오류 본문은 항상 `{"status":400,"message":"요청이 올바르지 않습니다."}` 형태입니다. 잘못된 입력과 JSON은 400, 인증 없음·잘못된 토큰·로그인 실패는 401, 타인 글·댓글 수정/삭제는 403, 없는 글·댓글은 404, 중복 이메일은 409입니다. 예상치 못한 서버 오류는 같은 모양의 500으로 답합니다.

## 설계

- **JWT 선택:** 서버에 로그인 세션을 저장하지 않고 API 호출마다 Bearer 토큰으로 사용자를 확인합니다. 비밀번호는 BCrypt로 해시해 저장하며 회원 응답이나 로그인 응답에 포함하지 않습니다.
- **관계:** 글은 작성자 회원을, 댓글은 작성자 회원과 글을 각각 `ManyToOne`으로 참조합니다. 생성·수정 시각은 서버가 기록합니다. 요청·응답에는 엔티티 대신 DTO를 사용합니다.
- **글 목록:** `PostRepository.findSummaries`의 집계 쿼리가 글, 작성자 닉네임, 댓글 수를 한 번에 조회하고 별도의 전체 건수 쿼리로 페이지를 만듭니다. 글마다 작성자나 댓글을 추가 조회하지 않습니다. `createdAt DESC, id DESC`로 최신순을 고정합니다. 통합 테스트에서 여러 글의 목록 조회에 사용한 SQL 문 수가 최대 2개인지 확인합니다.
- **글 삭제:** 글을 삭제할 권한을 확인한 뒤 같은 트랜잭션에서 해당 댓글을 먼저 모두 삭제하고 글을 삭제합니다. 댓글은 글과 함께 영구 삭제됩니다.
- **주소와 상태 코드:** 복수형 리소스 경로(`/posts`, `/comments`)를 쓰고 생성은 POST/201, 조회는 GET/200, 전체 수정은 PUT/200, 삭제는 DELETE/204로 정했습니다. 인증·권한·대상 부재는 각각 401·403·404로 구별합니다.
- **DB:** H2 파일 DB를 골라 별도 설치 없이 한 명령으로 실행하면서 서버 재시작 후에도 데이터를 유지합니다.

## 실행 결과

아래는 새 DB에서 순서대로 호출한 예시입니다. 긴 JWT와 시각은 일부 생략했습니다.

```bash
curl -i -X POST http://localhost:8081/members/signup -H 'Content-Type: application/json' -d '{"email":"demo@example.com","password":"password123","nickname":"Kim"}'
# HTTP 201
# {"id":1,"email":"demo@example.com","nickname":"Kim","createdAt":"2026-09-27T22:16:51"}

curl -i -X POST http://localhost:8081/auth/login -H 'Content-Type: application/json' -d '{"email":"demo@example.com","password":"password123"}'
# HTTP 200
# {"accessToken":"<JWT>","tokenType":"Bearer"}
```

로그인 응답의 토큰을 `TOKEN` 변수에 넣고 계속 호출합니다.

```bash
curl -i -X POST http://localhost:8081/posts -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"title":"First post","content":"Hello board"}'
# HTTP 201
# {"id":1,"title":"First post","content":"Hello board","authorNickname":"Kim","createdAt":"...","updatedAt":"..."}

curl -i -X POST http://localhost:8081/posts/1/comments -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"content":"First comment"}'
# HTTP 201
# {"id":1,"postId":1,"content":"First comment","authorNickname":"Kim","createdAt":"...","updatedAt":"..."}

curl -i 'http://localhost:8081/posts?page=0&size=10'
# HTTP 200
# {"content":[{"id":1,"title":"First post","authorNickname":"Kim","commentCount":1,"createdAt":"...","updatedAt":"..."}],"page":0,"size":10,"totalElements":1,"totalPages":1}

curl -i -X POST http://localhost:8081/posts -H 'Content-Type: application/json' -d '{"title":"No token","content":"Denied"}'
# HTTP 401
# {"status":401,"message":"로그인이 필요합니다."}
```

403 확인을 위해 다른 이메일로 가입·로그인하고 그 토큰을 `OTHER_TOKEN`에 넣습니다.

```bash
curl -i -X POST http://localhost:8081/members/signup -H 'Content-Type: application/json' -d '{"email":"other@example.com","password":"password123","nickname":"Lee"}'
# HTTP 201
# {"id":2,"email":"other@example.com","nickname":"Lee","createdAt":"..."}

curl -i -X POST http://localhost:8081/auth/login -H 'Content-Type: application/json' -d '{"email":"other@example.com","password":"password123"}'
# HTTP 200
# {"accessToken":"<OTHER_JWT>","tokenType":"Bearer"}

curl -i -X PUT http://localhost:8081/posts/1 -H "Authorization: Bearer $OTHER_TOKEN" -H 'Content-Type: application/json' -d '{"title":"Changed","content":"Denied"}'
# HTTP 403
# {"status":403,"message":"작성자만 변경할 수 있습니다."}
```
