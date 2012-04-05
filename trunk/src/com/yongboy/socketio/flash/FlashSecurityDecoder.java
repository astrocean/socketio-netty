package com.yongboy.socketio.flash;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.jboss.netty.handler.codec.replay.VoidEnum;
import org.jboss.netty.util.CharsetUtil;

/**
 * 
 * @author nieyong
 * @time 2012-3-29
 * @version 1.0
 */
public class FlashSecurityDecoder extends ReplayingDecoder<VoidEnum> {
	private static final ChannelBuffer requestBuffer = ChannelBuffers
			.copiedBuffer("<policy-file-request/>", CharsetUtil.UTF_8);

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buffer, VoidEnum state) {

		ChannelBuffer data = buffer.readBytes(requestBuffer.readableBytes());
		if (data.equals(requestBuffer)) {
			return data;
		}
		channel.close();

		return null;
	}
}