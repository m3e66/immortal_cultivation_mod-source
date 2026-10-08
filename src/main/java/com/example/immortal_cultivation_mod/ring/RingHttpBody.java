package com.example.immortal_cultivation_mod.ring;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Cancels oversized responses while receiving them, rather than buffering unlimited provider output. */
final class RingHttpBody implements HttpResponse.BodySubscriber<String> {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final CompletableFuture<String> body = new CompletableFuture<>();
    private Flow.Subscription subscription;
    static HttpResponse.BodyHandler<String> handler() { return response -> new RingHttpBody(); }
    @Override public CompletionStage<String> getBody() { return body; }
    @Override public void onSubscribe(Flow.Subscription value) {
        if (subscription != null) { value.cancel(); return; }
        subscription = value; value.request(1);
    }
    @Override public void onNext(List<ByteBuffer> values) {
        for (ByteBuffer value : values) {
            if (value.remaining() > 65536 - bytes.size()) {
                subscription.cancel(); body.completeExceptionally(new IllegalArgumentException("Response too large")); return;
            }
            byte[] chunk = new byte[value.remaining()]; value.get(chunk); bytes.writeBytes(chunk);
        }
        subscription.request(1);
    }
    @Override public void onError(Throwable error) { body.completeExceptionally(error); }
    @Override public void onComplete() { body.complete(bytes.toString(StandardCharsets.UTF_8)); }
}
