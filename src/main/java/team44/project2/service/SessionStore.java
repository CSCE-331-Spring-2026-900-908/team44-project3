package team44.project2.service;

import jakarta.enterprise.context.ApplicationScoped;
import team44.project2.model.Employee;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SessionStore {

    private final ConcurrentHashMap<String, Employee> sessions = new ConcurrentHashMap<>();

    public String createSession(Employee employee) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, employee);
        return token;
    }

    public Employee getSession(String token) {
        if (token == null) return null;
        return sessions.get(token);
    }

    public void removeSession(String token) {
        if (token != null) sessions.remove(token);
    }
}
