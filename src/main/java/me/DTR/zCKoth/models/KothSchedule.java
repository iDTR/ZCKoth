package me.DTR.zCKoth.models;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Representa una programación de activación automática para un KOTH
 */
public class KothSchedule {

    private final UUID id;
    private final String kothName;
    private final LocalTime time;
    private final List<DayOfWeek> days;
    private LocalDateTime lastExecution;
    private boolean enabled;

    /**
     * Constructor para una nueva programación
     *
     * @param kothName Nombre del KOTH a activar
     * @param time Hora del día para activarlo (HH:MM)
     * @param days Días de la semana en que se activará
     */
    public KothSchedule(String kothName, LocalTime time, List<DayOfWeek> days) {
        this.id = UUID.randomUUID();
        this.kothName = kothName;
        this.time = time;
        this.days = new ArrayList<>(days);
        this.lastExecution = null;
        this.enabled = true;
    }

    /**
     * Constructor para cargar una programación desde el almacenamiento
     */
    public KothSchedule(Map<String, Object> map) {
        this.id = UUID.fromString((String) map.get("id"));
        this.kothName = (String) map.get("kothName");

        // Cargar la hora
        String timeStr = (String) map.get("time");
        this.time = LocalTime.parse(timeStr);

        // Cargar los días
        this.days = new ArrayList<>();
        List<String> daysStr = (List<String>) map.get("days");
        for (String day : daysStr) {
            this.days.add(DayOfWeek.valueOf(day));
        }

        // Cargar última ejecución si existe
        String lastExecStr = (String) map.get("lastExecution");
        this.lastExecution = lastExecStr != null ?
                LocalDateTime.parse(lastExecStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;

        this.enabled = (boolean) map.get("enabled");
    }

    /**
     * Convierte la programación a un mapa para almacenamiento
     */
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id.toString());
        map.put("kothName", kothName);
        map.put("time", time.toString());

        // Convertir días a strings
        List<String> daysStr = new ArrayList<>();
        for (DayOfWeek day : days) {
            daysStr.add(day.name());
        }
        map.put("days", daysStr);

        // Guardar última ejecución si existe
        if (lastExecution != null) {
            map.put("lastExecution", lastExecution.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        map.put("enabled", enabled);
        return map;
    }

    /**
     * Verifica si la programación debe ejecutarse ahora
     *
     * @param currentTime La hora actual
     * @return true si debe ejecutarse, false en caso contrario
     */
    public boolean shouldExecute(LocalDateTime currentTime) {
        if (!enabled) {
            return false;
        }

        // Verificar si es el día correcto
        DayOfWeek today = currentTime.getDayOfWeek();
        if (!days.contains(today)) {
            return false;
        }

        // Verificar si es la hora correcta (margen de 1 minuto)
        LocalTime currentHour = currentTime.toLocalTime();
        long diffMinutes = Math.abs(time.toSecondOfDay() - currentHour.toSecondOfDay()) / 60;
        if (diffMinutes > 1) {
            return false;
        }

        // Verificar si ya se ejecutó hoy
        if (lastExecution != null) {
            LocalDateTime todayStart = LocalDateTime.of(currentTime.toLocalDate(), LocalTime.MIN);
            if (lastExecution.isAfter(todayStart)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Registra que la programación se ha ejecutado
     */
    public void markExecuted() {
        this.lastExecution = LocalDateTime.now();
    }

    // Getters y setters

    public UUID getId() {
        return id;
    }

    public String getKothName() {
        return kothName;
    }

    public LocalTime getTime() {
        return time;
    }

    public List<DayOfWeek> getDays() {
        return new ArrayList<>(days);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getLastExecution() {
        return lastExecution;
    }

    /**
     * Devuelve una representación amigable de los días programados
     */
    public String getDaysString() {
        if (days.size() == 7) {
            return "Todos los días";
        }

        StringBuilder sb = new StringBuilder();
        for (DayOfWeek day : days) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            switch (day) {
                case MONDAY:
                    sb.append("Lunes");
                    break;
                case TUESDAY:
                    sb.append("Martes");
                    break;
                case WEDNESDAY:
                    sb.append("Miércoles");
                    break;
                case THURSDAY:
                    sb.append("Jueves");
                    break;
                case FRIDAY:
                    sb.append("Viernes");
                    break;
                case SATURDAY:
                    sb.append("Sábado");
                    break;
                case SUNDAY:
                    sb.append("Domingo");
                    break;
            }
        }

        return sb.toString();
    }

    /**
     * Devuelve una representación en texto de la programación
     */
    @Override
    public String toString() {
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
        return kothName + " - " + time.format(timeFormat) + " - " + getDaysString() +
                (enabled ? " (Activo)" : " (Inactivo)");
    }
}