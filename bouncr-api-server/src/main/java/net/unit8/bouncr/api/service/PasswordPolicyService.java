package net.unit8.bouncr.api.service;

import kotowari.restful.data.Problem;
import net.unit8.bouncr.component.config.PasswordPolicy;

import javax.persistence.EntityManager;

public class PasswordPolicyService {
    private EntityManager em;
    private PasswordPolicy policy;

    public PasswordPolicyService(PasswordPolicy policy, EntityManager em) {
        this.em = em;
        this.policy = policy;
    }

    public Problem.Violation validate(String password) {
        if (password.length() > policy.getMaxLength()) {
            return new Problem.Violation("passwrod", "must be less than " + policy.getMaxLength() + " characters");
        }

        if (password.length() < policy.getMinLength()) {
            return new Problem.Violation("passwrod", "must be greater than " + policy.getMaxLength() + " characters");
        }

        if (!policy.getPattern().matcher(password).matches()) {
            return new Problem.Violation("password", "");
        }

        return null;
    }
}
