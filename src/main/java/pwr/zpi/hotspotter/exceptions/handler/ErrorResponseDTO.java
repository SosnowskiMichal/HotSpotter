package pwr.zpi.hotspotter.exceptions.handler;


public record ErrorResponseDTO(String error, String message, int errorCode) {
}
