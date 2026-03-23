import { createBrowserRouter } from 'react-router-dom';
import { RootLayout } from '@/layouts/root-layout';
import { RequireAuth } from '@/auth/require-auth';
import { RequirePermission } from '@/auth/require-permission';
import { ROUTES } from './route-paths';

import { SignInPage } from '@/features/sign-in/sign-in-page';
import { SignUpPage } from '@/features/sign-up/sign-up-page';
import { OidcCallbackPage } from '@/features/sign-in-by-oidc/oidc-callback-page';
import { HomePage } from '@/features/home/home-page';
import { ChangePasswordPage } from '@/features/change-password/change-password-page';
import { ChangeProfilePage } from '@/features/change-profile/change-profile-page';
import { ResetChallengePage } from '@/features/reset-password/reset-challenge-page';
import { ResetPasswordPage } from '@/features/reset-password/reset-password-page';
import { EmailVerificationPage } from '@/features/email-verification/email-verification-page';
import { AuditPage } from '@/features/audit/audit-page';
import { UsersAdminPage } from '@/features/users/users-admin-page';
import { GroupsAdminPage } from '@/features/groups/groups-admin-page';
import { RolesAdminPage } from '@/features/roles/roles-admin-page';
import { PermissionsAdminPage } from '@/features/permissions/permissions-admin-page';
import { ApplicationsAdminPage } from '@/features/applications/applications-admin-page';
import { RealmsAdminPage } from '@/features/realms/realms-admin-page';
import { OidcProvidersAdminPage } from '@/features/oidc-providers/oidc-providers-admin-page';
import { OidcApplicationsAdminPage } from '@/features/oidc-applications/oidc-applications-admin-page';
import { InvitationsAdminPage } from '@/features/invitations/invitations-admin-page';
import { NotFoundPage } from '@/features/not-found/not-found-page';

export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      // Public routes
      { path: ROUTES.SIGN_IN, element: <SignInPage /> },
      { path: ROUTES.SIGN_UP, element: <SignUpPage /> },
      { path: ROUTES.SIGN_IN_BY_OIDC, element: <OidcCallbackPage /> },
      { path: ROUTES.RESET_PASSWORD_CHALLENGE, element: <ResetChallengePage /> },
      { path: ROUTES.RESET_PASSWORD, element: <ResetPasswordPage /> },
      { path: ROUTES.EMAIL_VERIFICATION, element: <EmailVerificationPage /> },

      // Authenticated routes
      {
        element: <RequireAuth />,
        children: [
          { path: ROUTES.HOME, element: <HomePage /> },
          { path: ROUTES.CHANGE_PASSWORD, element: <ChangePasswordPage /> },
          { path: ROUTES.CHANGE_PROFILE, element: <ChangeProfilePage /> },

          // Admin routes with permission guards
          {
            element: <RequirePermission permissions={['any_user:read', 'user:read']} />,
            children: [
              { path: ROUTES.USERS, element: <UsersAdminPage /> },
              { path: ROUTES.AUDIT, element: <AuditPage /> },
            ],
          },
          {
            element: <RequirePermission permissions={['any_group:read', 'group:read']} />,
            children: [
              { path: ROUTES.GROUPS, element: <GroupsAdminPage /> },
            ],
          },
          {
            element: <RequirePermission permissions={['any_application:read', 'application:read']} />,
            children: [
              { path: ROUTES.APPLICATIONS, element: <ApplicationsAdminPage /> },
              { path: ROUTES.REALMS, element: <RealmsAdminPage /> },
            ],
          },
          {
            element: <RequirePermission permissions={['any_role:read', 'role:read']} />,
            children: [
              { path: ROUTES.ROLES, element: <RolesAdminPage /> },
            ],
          },
          {
            element: <RequirePermission permissions={['any_permission:read', 'permission:read']} />,
            children: [
              { path: ROUTES.PERMISSIONS, element: <PermissionsAdminPage /> },
            ],
          },
          {
            element: <RequirePermission permissions={['oidc_provider:read']} />,
            children: [
              { path: ROUTES.OIDC_PROVIDERS, element: <OidcProvidersAdminPage /> },
            ],
          },
          {
            element: <RequirePermission permissions={['oidc_application:read']} />,
            children: [
              { path: ROUTES.OIDC_APPLICATIONS, element: <OidcApplicationsAdminPage /> },
            ],
          },
          {
            element: <RequirePermission permissions={['invitation:create']} />,
            children: [
              { path: ROUTES.INVITATIONS, element: <InvitationsAdminPage /> },
            ],
          },
        ],
      },

      // Catch-all
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);
