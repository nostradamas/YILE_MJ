package game.zjh.scene.net;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.isnowfox.core.net.message.Message;
import com.isnowfox.core.net.message.MessageFactory;
import com.isnowfox.game.proxy.message.LogoutPxMsg;
import com.isnowfox.game.proxy.message.SinglePxMsg;

import game.scene.ServerRuntimeException;
import game.zjh.scene.room.ZJHRoomService;
import io.netty.channel.Channel;


final class ZGateway {
    private static final Logger log = LoggerFactory.getLogger(ZGateway.class);


    @Autowired
    private MessageFactory messageFactory;
    @Autowired
    private ZJHRoomService ZJHRoomService;

    private Channel channel;
    private int id;
    private final ReentrantLock lock = new ReentrantLock();

    ZGateway() {

    }


    void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "Gateway [channel=" + channel + "]";
    }

    void setId(int id) {
        this.id = id;
    }

    int getId() {
        return id;
    }

    void close() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            channel.close();
        } finally {
            lock.unlock();
        }
    }

    void send(short userSessionId, Message msg) {
        checkMsg(msg);
        SinglePxMsg.EncodeWrapper w = new SinglePxMsg.EncodeWrapper(userSessionId, msg);
        log.info("发送{}", w);
        channel.writeAndFlush(w);
    }

    private void checkMsg(Message msg) {
        Message testMsg;
        try {
            testMsg = messageFactory.getMessage(msg.getMessageType(), msg.getMessageId());
        } catch (Exception e) {
            throw new ServerRuntimeException("错误的消息", e);
        }
        if (!testMsg.getClass().isAssignableFrom(msg.getClass())) {
            throw new ServerRuntimeException("错误的消息");
        }
    }

    void unReg(short sessionId) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            channel.writeAndFlush(new LogoutPxMsg(sessionId));
            log.info("注销Session:{},[gateway:{}] ", sessionId, this.id);
        } finally {
            lock.unlock();
        }
    }
}
