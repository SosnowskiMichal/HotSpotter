package pwr.zpi.hotspotter.common.exception.handler;


public record ErrorResponseDTO(String error, String message, int errorCode) {
}
