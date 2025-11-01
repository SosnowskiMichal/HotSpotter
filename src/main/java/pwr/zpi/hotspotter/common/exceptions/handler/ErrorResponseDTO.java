package pwr.zpi.hotspotter.common.exceptions.handler;


public record ErrorResponseDTO(String error, String message, int errorCode) {
}
