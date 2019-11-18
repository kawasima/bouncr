export ADMIN_CREDENTIAL="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInVpZCI6IjEiLCJwZXJtaXNzaW9ucyI6WyJhbnlfdXNlcjpyZWFkIiwiYW55X3VzZXI6Y3JlYXRlIiwiYW55X3VzZXI6dXBkYXRlIiwiYW55X3VzZXI6ZGVsZXRlIiwiYW55X3VzZXI6bG9jayIsImFueV91c2VyOnVubG9jayIsImFueV9ncm91cDpyZWFkIiwiYW55X2dyb3VwOmNyZWF0ZSIsImFueV9ncm91cDp1cGRhdGUiLCJhbnlfZ3JvdXA6ZGVsZXRlIiwiYW55X2FwcGxpY2F0aW9uOnJlYWQiLCJhbnlfYXBwbGljYXRpb246Y3JlYXRlIiwiYW55X2FwcGxpY2F0aW9uOnVwZGF0ZSIsImFueV9hcHBsaWNhdGlvbjpkZWxldGUiLCJhbnlfcmVhbG06cmVhZCIsImFueV9yZWFsbTpjcmVhdGUiLCJhbnlfcmVhbG06dXBkYXRlIiwiYW55X3JlYWxtOmRlbGV0ZSIsImFueV9yb2xlOnJlYWQiLCJhbnlfcm9sZTpjcmVhdGUiLCJhbnlfcm9sZTp1cGRhdGUiLCJhbnlfcm9sZTpkZWxldGUiLCJhbnlfcGVybWlzc2lvbjpyZWFkIiwiYW55X3Blcm1pc3Npb246Y3JlYXRlIiwiYW55X3Blcm1pc3Npb246dXBkYXRlIiwiYW55X3Blcm1pc3Npb246ZGVsZXRlIiwiYXNzaWdubWVudHM6cmVhZCIsImFzc2lnbm1lbnRzOmNyZWF0ZSIsImFzc2lnbm1lbnRzOmRlbGV0ZSIsIm9pZGNfYXBwbGljYXRpb246cmVhZCIsIm9pZGNfYXBwbGljYXRpb246Y3JlYXRlIiwib2lkY19hcHBsaWNhdGlvbjp1cGRhdGUiLCJvaWRjX2FwcGxpY2F0aW9uOmRlbGV0ZSIsIm9pZGNfcHJvdmlkZXI6cmVhZCIsIm9pZGNfcHJvdmlkZXI6Y3JlYXRlIiwib2lkY19wcm92aWRlcjp1cGRhdGUiLCJvaWRjX3Byb3ZpZGVyOmRlbGV0ZSIsImludml0YXRpb246Y3JlYXRlIl19.zunhcwh6pK4AeSSXWKGy9Tlbsf_w27vGAtmknmAeHw8"

# ユーザ新規登録
curl -i -XPOST -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" -d '{"account": "user1", "name": "User1", "email": "user1@example.com"}' http://localhost:3005/bouncr/api/users

# ユーザの確認
curl -i -XGET -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}"  http://localhost:3005/bouncr/api/users

# ユーザのパスワード設定
curl -i -XPOST -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" -d '{"account": "user1", "password":"hogehoge"}' http://localhost:3005/bouncr/api/password_credential

# グループの確認
curl -i -XGET -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}"  http://localhost:3005/bouncr/api/user/user1\?embed=\(groups\)

# グループ作成
curl -i -XPOST -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" -d '{"name": "group1", "description": "group No1"}' http://localhost:3005/bouncr/api/groups

# グループにユーザを追加する
curl -i -XPOST -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" -d '{"users": ["user1"]}' http://localhost:3005/bouncr/api/group/group1/users

# グループからユーザを削除する
curl -i -XDELETE -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" -d '{"users": ["user1"]}' http://localhost:3005/bouncr/api/group/group1/users

# ロールを作る
curl -i -XPOST -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" -d '{"name": "hoge_users", "description": "Hoge Users"}' http://localhost:3005/bouncr/api/roles

# ロールの確認
curl -i -XGET  -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" http://localhost:3005/bouncr/api/role/hoge_users

# アプリケーションを作る
curl -i -XPOST -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" -d '{"name": "app1", "description": "Application1", "virtual_path": "/app1", "pass_to": "http://localhost:3008", "top_page": "http://localhost:3000/app1"}' http://localhost:3005/bouncr/api/applications

# アプリケーションを確認
curl -i -XGET -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" 'http://localhost:3005/bouncr/api/application/app1?embed=(realms)'

# 作成したユーザでのサインイン
curl -i -XPOST -H 'content-type: application/json' -H 'accept: application/json' -d '{"account": "user1", "password": "hogehoge"}' 'http://localhost:3005/bouncr/api/sign_in'

## -----------------------
## OpenID Connect Provider
## -----------------------

# OpenIDプロバイダ一覧
curl -i -XGET -H -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" http://localhost:3005/bouncr/api/oidc_providers

# OpenIDプロバイダ作成
curl -i -XPOST -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" -d '{"name": "google", "client_id":"xxxx", "client_secret":"secret","scope":"openid email","response_type":"code","authorization_endpoint":"https://accounts.google.com/o/oauth2/v2/auth","token_endpoint":"https://www.googleapis.com/oauth2/v4/token", "token_endpoint_auth_method":"POST"}' http://localhost:3005/bouncr/api/oidc_providers

# Replから関連付け
/sql INSERT INTO oidc_users(oidc_provider_id,user_id,oidc_sub) VALUES (1,1,'google_admin')

curl -i -XGET -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" 'http://localhost:3005/bouncr/api/user/admin?embed=(openid_connect_providers)'

# OpenId Invitation
/sql INSERT INTO invitations(email, code, invited_at) VALUES ('kawasima1016@gmail.com','12345678','2019-01-01')
/sql INSERT INTO oidc_invitations(invitation_id, oidc_provider_id, oidc_payload) VALUES (1,1,'eyJlbWFpbCI6Imthd2FzaW1hMTAxNkBnbWFpbC5jb20ifQ')

curl -i -XGET  -H 'content-type: application/json' -H 'accept: application/json' -H "X-Bouncr-Credential: ${ADMIN_CREDENTIAL}" 'http://localhost:3005/bouncr/api/invitation/12345678'
