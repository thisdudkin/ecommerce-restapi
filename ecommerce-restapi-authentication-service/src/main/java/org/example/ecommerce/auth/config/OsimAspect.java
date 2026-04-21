package org.example.ecommerce.auth.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
public class OsimAspect {

    private final EntityManagerFactory emf;

    public OsimAspect(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Around("@annotation(org.example.ecommerce.auth.annotation.Osim)")
    public Object performOsimly(ProceedingJoinPoint pjp) throws Throwable {
        if (TransactionSynchronizationManager.hasResource(emf))
            return pjp.proceed();
        try (EntityManager em = emf.createEntityManager()) {
            EntityManagerHolder emHolder = new EntityManagerHolder(em);
            TransactionSynchronizationManager.bindResource(emf, emHolder);
            return pjp.proceed();
        } finally {
            TransactionSynchronizationManager.unbindResource(emf);
        }
    }

}
