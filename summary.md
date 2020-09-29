В рамках утилиты wrk сервер имеет следующие показатели по производительности. (wrk -t1 -c10 -d10s http://localhost:8080/v0/status)
![alt text](wrk.png "wrk")
Flamegraph выглядит следующим образом (./profiler.sh -d 5 -f flamegraph.svg 27555)
![alt text](flamegraph.svg "flamegraph")

Логи после профилирования процессора (./profiler.sh -d 5 29233)

Started [cpu] profiling
--- Execution profile ---
Total samples       : 676
not_walkable_Java   : 1 (0,15%)

Frame buffer usage  : 0.7403%

--- 142511144 ns (2.07%), 14 samples
  [ 0] java.lang.StringLatin1.indexOf
  [ 1] java.lang.String.indexOf
  [ 2] java.lang.String.indexOf
  [ 3] one.nio.http.Request.<init>
  [ 4] one.nio.http.HttpSession.parseRequest
  [ 5] one.nio.http.HttpSession.processHttpBuffer
  [ 6] one.nio.http.HttpSession.processRead
  [ 7] one.nio.net.Session.process
  [ 8] one.nio.server.SelectorThread.run

--- 122208116 ns (1.77%), 12 samples
  [ 0] one.nio.util.Utf8.startsWith
  [ 1] one.nio.http.HttpSession.parseRequest
  [ 2] one.nio.http.HttpSession.processHttpBuffer
  [ 3] one.nio.http.HttpSession.processRead
  [ 4] one.nio.net.Session.process
  [ 5] one.nio.server.SelectorThread.run

--- 101940305 ns (1.48%), 10 samples
  [ 0] java.lang.StringUTF16.checkIndex
  [ 1] java.lang.StringUTF16.charAt
  [ 2] java.lang.String.charAt
  [ 3] one.nio.util.Utf8.length
  [ 4] one.nio.util.ByteArrayBuilder.append
  [ 5] one.nio.http.Response.toBytes
  [ 6] one.nio.http.HttpSession.writeResponse
  [ 7] one.nio.http.HttpSession.sendResponse
  [ 8] RequestHandler2_status.handleRequest
  [ 9] one.nio.http.HttpServer.handleRequest
  [10] one.nio.http.HttpSession.handleParsedRequest
  [11] one.nio.http.HttpSession.processHttpBuffer
  [12] one.nio.http.HttpSession.processRead
  [13] one.nio.net.Session.process
  [14] one.nio.server.SelectorThread.run

--- 101837817 ns (1.48%), 10 samples
  [ 0] one.nio.http.Response.toBytes
  [ 1] one.nio.http.HttpSession.writeResponse
  [ 2] one.nio.http.HttpSession.sendResponse
  [ 3] RequestHandler2_status.handleRequest
  [ 4] one.nio.http.HttpServer.handleRequest
  [ 5] one.nio.http.HttpSession.handleParsedRequest
  [ 6] one.nio.http.HttpSession.processHttpBuffer
  [ 7] one.nio.http.HttpSession.processRead
  [ 8] one.nio.net.Session.process
  [ 9] one.nio.server.SelectorThread.run

--- 91799386 ns (1.33%), 9 samples
  [ 0] one.nio.net.NativeSelector.epollWait
  [ 1] one.nio.net.NativeSelector.select
  [ 2] one.nio.server.SelectorThread.run

--- 91673151 ns (1.33%), 9 samples
  [ 0] __tcp_transmit_skb_[k]
  [ 1] tcp_write_xmit_[k]
  [ 2] __tcp_push_pending_frames_[k]
  [ 3] tcp_push_[k]
  [ 4] tcp_sendmsg_locked_[k]
  [ 5] tcp_sendmsg_[k]
  [ 6] inet6_sendmsg_[k]
  [ 7] sock_sendmsg_[k]
  [ 8] __sys_sendto_[k]
  [ 9] __x64_sys_sendto_[k]
  [10] do_syscall_64_[k]
  [11] entry_SYSCALL_64_after_hwframe_[k]
  [12] __libc_send
  [13] one.nio.net.NativeSocket.write
  [14] one.nio.net.Session$ArrayQueueItem.write
  [15] one.nio.net.Session.write
  [16] one.nio.net.Session.write
  [17] one.nio.http.HttpSession.writeResponse
  [18] one.nio.http.HttpSession.sendResponse
  [19] RequestHandler2_status.handleRequest
  [20] one.nio.http.HttpServer.handleRequest
  [21] one.nio.http.HttpSession.handleParsedRequest
  [22] one.nio.http.HttpSession.processHttpBuffer
  [23] one.nio.http.HttpSession.processRead
  [24] one.nio.net.Session.process
  [25] one.nio.server.SelectorThread.run

--- 91653576 ns (1.33%), 9 samples
  [ 0] clock_gettime
  [ 1] [unknown]
  [ 2] [unknown]
  [ 3] one.nio.net.NativeSelector.epollWait
  [ 4] one.nio.net.NativeSelector.select
  [ 5] one.nio.server.SelectorThread.run

--- 91648143 ns (1.33%), 9 samples
  [ 0] __lock_text_start_[k]
  [ 1] __wake_up_common_lock_[k]
  [ 2] __wake_up_sync_key_[k]
  [ 3] sock_def_readable_[k]
  [ 4] tcp_data_ready_[k]
  [ 5] tcp_rcv_established_[k]
  [ 6] tcp_v4_do_rcv_[k]
  [ 7] tcp_v4_rcv_[k]
  [ 8] ip_protocol_deliver_rcu_[k]
  [ 9] ip_local_deliver_finish_[k]
  [10] ip_local_deliver_[k]
  [11] ip_rcv_finish_[k]
  [12] ip_rcv_[k]
  [13] __netif_receive_skb_one_core_[k]
  [14] __netif_receive_skb_[k]
  [15] process_backlog_[k]
  [16] net_rx_action_[k]
  [17] __softirqentry_text_start_[k]
  [18] do_softirq_own_stack_[k]
  [19] do_softirq.part.20_[k]
  [20] __local_bh_enable_ip_[k]
  [21] ip_finish_output2_[k]
  [22] __ip_finish_output_[k]
  [23] ip_finish_output_[k]
  [24] ip_output_[k]
  [25] ip_local_out_[k]
  [26] __ip_queue_xmit_[k]
  [27] ip_queue_xmit_[k]
  [28] __tcp_transmit_skb_[k]
  [29] tcp_write_xmit_[k]
  [30] __tcp_push_pending_frames_[k]
  [31] tcp_push_[k]
  [32] tcp_sendmsg_locked_[k]
  [33] tcp_sendmsg_[k]
  [34] inet6_sendmsg_[k]
  [35] sock_sendmsg_[k]
  [36] __sys_sendto_[k]
  [37] __x64_sys_sendto_[k]
  [38] do_syscall_64_[k]
  [39] entry_SYSCALL_64_after_hwframe_[k]
  [40] __libc_send
  [41] one.nio.net.NativeSocket.write
  [42] one.nio.net.Session$ArrayQueueItem.write
  [43] one.nio.net.Session.write
  [44] one.nio.net.Session.write
  [45] one.nio.http.HttpSession.writeResponse
  [46] one.nio.http.HttpSession.sendResponse
  [47] RequestHandler2_status.handleRequest
  [48] one.nio.http.HttpServer.handleRequest
  [49] one.nio.http.HttpSession.handleParsedRequest
  [50] one.nio.http.HttpSession.processHttpBuffer
  [51] one.nio.http.HttpSession.processRead
  [52] one.nio.net.Session.process
  [53] one.nio.server.SelectorThread.run

--- 91626865 ns (1.33%), 9 samples
  [ 0] __ksize_[k]
  [ 1] __alloc_skb_[k]
  [ 2] sk_stream_alloc_skb_[k]
  [ 3] tcp_sendmsg_locked_[k]
  [ 4] tcp_sendmsg_[k]
  [ 5] inet6_sendmsg_[k]
  [ 6] sock_sendmsg_[k]
  [ 7] __sys_sendto_[k]
  [ 8] __x64_sys_sendto_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __libc_send
  [12] one.nio.net.NativeSocket.write
  [13] one.nio.net.Session$ArrayQueueItem.write
  [14] one.nio.net.Session.write
  [15] one.nio.net.Session.write
  [16] one.nio.http.HttpSession.writeResponse
  [17] one.nio.http.HttpSession.sendResponse
  [18] RequestHandler2_status.handleRequest
  [19] one.nio.http.HttpServer.handleRequest
  [20] one.nio.http.HttpSession.handleParsedRequest
  [21] one.nio.http.HttpSession.processHttpBuffer
  [22] one.nio.http.HttpSession.processRead
  [23] one.nio.net.Session.process
  [24] one.nio.server.SelectorThread.run

--- 81584450 ns (1.18%), 8 samples
  [ 0] epoll_wait
  [ 1] [unknown]
  [ 2] one.nio.net.NativeSelector.epollWait
  [ 3] one.nio.net.NativeSelector.select
  [ 4] one.nio.server.SelectorThread.run

--- 81556485 ns (1.18%), 8 samples
  [ 0] tcp_ack_[k]
  [ 1] tcp_rcv_established_[k]
  [ 2] tcp_v4_do_rcv_[k]
  [ 3] tcp_v4_rcv_[k]
  [ 4] ip_protocol_deliver_rcu_[k]
  [ 5] ip_local_deliver_finish_[k]
  [ 6] ip_local_deliver_[k]
  [ 7] ip_rcv_finish_[k]
  [ 8] ip_rcv_[k]
  [ 9] __netif_receive_skb_one_core_[k]
  [10] __netif_receive_skb_[k]
  [11] process_backlog_[k]
  [12] net_rx_action_[k]
  [13] __softirqentry_text_start_[k]
  [14] do_softirq_own_stack_[k]
  [15] do_softirq.part.20_[k]
  [16] __local_bh_enable_ip_[k]
  [17] ip_finish_output2_[k]
  [18] __ip_finish_output_[k]
  [19] ip_finish_output_[k]
  [20] ip_output_[k]
  [21] ip_local_out_[k]
  [22] __ip_queue_xmit_[k]
  [23] ip_queue_xmit_[k]
  [24] __tcp_transmit_skb_[k]
  [25] tcp_write_xmit_[k]
  [26] __tcp_push_pending_frames_[k]
  [27] tcp_push_[k]
  [28] tcp_sendmsg_locked_[k]
  [29] tcp_sendmsg_[k]
  [30] inet6_sendmsg_[k]
  [31] sock_sendmsg_[k]
  [32] __sys_sendto_[k]
  [33] __x64_sys_sendto_[k]
  [34] do_syscall_64_[k]
  [35] entry_SYSCALL_64_after_hwframe_[k]
  [36] __libc_send
  [37] one.nio.net.NativeSocket.write
  [38] one.nio.net.Session$ArrayQueueItem.write
  [39] one.nio.net.Session.write
  [40] one.nio.net.Session.write
  [41] one.nio.http.HttpSession.writeResponse
  [42] one.nio.http.HttpSession.sendResponse
  [43] RequestHandler2_status.handleRequest
  [44] one.nio.http.HttpServer.handleRequest
  [45] one.nio.http.HttpSession.handleParsedRequest
  [46] one.nio.http.HttpSession.processHttpBuffer
  [47] one.nio.http.HttpSession.processRead
  [48] one.nio.net.Session.process
  [49] one.nio.server.SelectorThread.run

--- 81535459 ns (1.18%), 8 samples
  [ 0] [vdso]
  [ 1] clock_gettime
  [ 2] clock_gettime
  [ 3] [unknown]
  [ 4] [unknown]
  [ 5] one.nio.net.NativeSelector.epollWait
  [ 6] one.nio.net.NativeSelector.select
  [ 7] one.nio.server.SelectorThread.run

--- 81515724 ns (1.18%), 8 samples
  [ 0] tcp_sendmsg_locked_[k]
  [ 1] tcp_sendmsg_[k]
  [ 2] inet6_sendmsg_[k]
  [ 3] sock_sendmsg_[k]
  [ 4] __sys_sendto_[k]
  [ 5] __x64_sys_sendto_[k]
  [ 6] do_syscall_64_[k]
  [ 7] entry_SYSCALL_64_after_hwframe_[k]
  [ 8] __libc_send
  [ 9] one.nio.net.NativeSocket.write
  [10] one.nio.net.Session$ArrayQueueItem.write
  [11] one.nio.net.Session.write
  [12] one.nio.net.Session.write
  [13] one.nio.http.HttpSession.writeResponse
  [14] one.nio.http.HttpSession.sendResponse
  [15] RequestHandler2_status.handleRequest
  [16] one.nio.http.HttpServer.handleRequest
  [17] one.nio.http.HttpSession.handleParsedRequest
  [18] one.nio.http.HttpSession.processHttpBuffer
  [19] one.nio.http.HttpSession.processRead
  [20] one.nio.net.Session.process
  [21] one.nio.server.SelectorThread.run

--- 81467530 ns (1.18%), 8 samples
  [ 0] __inet_lookup_established_[k]
  [ 1] tcp_v4_rcv_[k]
  [ 2] ip_protocol_deliver_rcu_[k]
  [ 3] ip_local_deliver_finish_[k]
  [ 4] ip_local_deliver_[k]
  [ 5] ip_rcv_finish_[k]
  [ 6] ip_rcv_[k]
  [ 7] __netif_receive_skb_one_core_[k]
  [ 8] __netif_receive_skb_[k]
  [ 9] process_backlog_[k]
  [10] net_rx_action_[k]
  [11] __softirqentry_text_start_[k]
  [12] do_softirq_own_stack_[k]
  [13] do_softirq.part.20_[k]
  [14] __local_bh_enable_ip_[k]
  [15] ip_finish_output2_[k]
  [16] __ip_finish_output_[k]
  [17] ip_finish_output_[k]
  [18] ip_output_[k]
  [19] ip_local_out_[k]
  [20] __ip_queue_xmit_[k]
  [21] ip_queue_xmit_[k]
  [22] __tcp_transmit_skb_[k]
  [23] tcp_write_xmit_[k]
  [24] __tcp_push_pending_frames_[k]
  [25] tcp_push_[k]
  [26] tcp_sendmsg_locked_[k]
  [27] tcp_sendmsg_[k]
  [28] inet6_sendmsg_[k]
  [29] sock_sendmsg_[k]
  [30] __sys_sendto_[k]
  [31] __x64_sys_sendto_[k]
  [32] do_syscall_64_[k]
  [33] entry_SYSCALL_64_after_hwframe_[k]
  [34] __libc_send
  [35] one.nio.net.NativeSocket.write
  [36] one.nio.net.Session$ArrayQueueItem.write
  [37] one.nio.net.Session.write
  [38] one.nio.net.Session.write
  [39] one.nio.http.HttpSession.writeResponse
  [40] one.nio.http.HttpSession.sendResponse
  [41] RequestHandler2_status.handleRequest
  [42] one.nio.http.HttpServer.handleRequest
  [43] one.nio.http.HttpSession.handleParsedRequest
  [44] one.nio.http.HttpSession.processHttpBuffer
  [45] one.nio.http.HttpSession.processRead
  [46] one.nio.net.Session.process
  [47] one.nio.server.SelectorThread.run

--- 71383571 ns (1.04%), 7 samples
  [ 0] __nf_conntrack_find_get?[nf_conntrack]_[k]
  [ 1] nf_conntrack_in?[nf_conntrack]_[k]
  [ 2] ipv4_conntrack_local?[nf_conntrack]_[k]
  [ 3] nf_hook_slow_[k]
  [ 4] __ip_local_out_[k]
  [ 5] ip_local_out_[k]
  [ 6] __ip_queue_xmit_[k]
  [ 7] ip_queue_xmit_[k]
  [ 8] __tcp_transmit_skb_[k]
  [ 9] tcp_write_xmit_[k]
  [10] __tcp_push_pending_frames_[k]
  [11] tcp_push_[k]
  [12] tcp_sendmsg_locked_[k]
  [13] tcp_sendmsg_[k]
  [14] inet6_sendmsg_[k]
  [15] sock_sendmsg_[k]
  [16] __sys_sendto_[k]
  [17] __x64_sys_sendto_[k]
  [18] do_syscall_64_[k]
  [19] entry_SYSCALL_64_after_hwframe_[k]
  [20] __libc_send
  [21] one.nio.net.NativeSocket.write
  [22] one.nio.net.Session$ArrayQueueItem.write
  [23] one.nio.net.Session.write
  [24] one.nio.net.Session.write
  [25] one.nio.http.HttpSession.writeResponse
  [26] one.nio.http.HttpSession.sendResponse
  [27] RequestHandler2_status.handleRequest
  [28] one.nio.http.HttpServer.handleRequest
  [29] one.nio.http.HttpSession.handleParsedRequest
  [30] one.nio.http.HttpSession.processHttpBuffer
  [31] one.nio.http.HttpSession.processRead
  [32] one.nio.net.Session.process
  [33] one.nio.server.SelectorThread.run

--- 71358705 ns (1.04%), 7 samples
  [ 0] tcp_recvmsg_[k]
  [ 1] inet6_recvmsg_[k]
  [ 2] sock_recvmsg_[k]
  [ 3] __sys_recvfrom_[k]
  [ 4] __x64_sys_recvfrom_[k]
  [ 5] do_syscall_64_[k]
  [ 6] entry_SYSCALL_64_after_hwframe_[k]
  [ 7] __GI___recv
  [ 8] one.nio.net.NativeSocket.read
  [ 9] one.nio.net.Session.read
  [10] one.nio.http.HttpSession.processRead
  [11] one.nio.net.Session.process
  [12] one.nio.server.SelectorThread.run

--- 71279815 ns (1.03%), 7 samples
  [ 0] java.util.HashMap.getNode
  [ 1] java.util.HashMap.get
  [ 2] one.nio.http.PathMapper.find
  [ 3] one.nio.http.HttpServer.handleRequest
  [ 4] one.nio.http.HttpSession.handleParsedRequest
  [ 5] one.nio.http.HttpSession.processHttpBuffer
  [ 6] one.nio.http.HttpSession.processRead
  [ 7] one.nio.net.Session.process
  [ 8] one.nio.server.SelectorThread.run

--- 61159353 ns (0.89%), 6 samples
  [ 0] aa_sk_perm_[k]
  [ 1] aa_sock_msg_perm_[k]
  [ 2] apparmor_socket_recvmsg_[k]
  [ 3] security_socket_recvmsg_[k]
  [ 4] sock_recvmsg_[k]
  [ 5] __sys_recvfrom_[k]
  [ 6] __x64_sys_recvfrom_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] __GI___recv
  [10] one.nio.net.NativeSocket.read
  [11] one.nio.net.Session.read
  [12] one.nio.http.HttpSession.processRead
  [13] one.nio.net.Session.process
  [14] one.nio.server.SelectorThread.run

--- 61095196 ns (0.89%), 6 samples
  [ 0] __kmalloc_node_track_caller_[k]
  [ 1] __kmalloc_reserve.isra.62_[k]
  [ 2] __alloc_skb_[k]
  [ 3] sk_stream_alloc_skb_[k]
  [ 4] tcp_sendmsg_locked_[k]
  [ 5] tcp_sendmsg_[k]
  [ 6] inet6_sendmsg_[k]
  [ 7] sock_sendmsg_[k]
  [ 8] __sys_sendto_[k]
  [ 9] __x64_sys_sendto_[k]
  [10] do_syscall_64_[k]
  [11] entry_SYSCALL_64_after_hwframe_[k]
  [12] __libc_send
  [13] one.nio.net.NativeSocket.write
  [14] one.nio.net.Session$ArrayQueueItem.write
  [15] one.nio.net.Session.write
  [16] one.nio.net.Session.write
  [17] one.nio.http.HttpSession.writeResponse
  [18] one.nio.http.HttpSession.sendResponse
  [19] RequestHandler2_status.handleRequest
  [20] one.nio.http.HttpServer.handleRequest
  [21] one.nio.http.HttpSession.handleParsedRequest
  [22] one.nio.http.HttpSession.processHttpBuffer
  [23] one.nio.http.HttpSession.processRead
  [24] one.nio.net.Session.process
  [25] one.nio.server.SelectorThread.run

--- 61092691 ns (0.89%), 6 samples
  [ 0] ipt_do_table?[ip_tables]_[k]
  [ 1] iptable_filter_hook?[iptable_filter]_[k]
  [ 2] nf_hook_slow_[k]
  [ 3] __ip_local_out_[k]
  [ 4] ip_local_out_[k]
  [ 5] __ip_queue_xmit_[k]
  [ 6] ip_queue_xmit_[k]
  [ 7] __tcp_transmit_skb_[k]
  [ 8] tcp_write_xmit_[k]
  [ 9] __tcp_push_pending_frames_[k]
  [10] tcp_push_[k]
  [11] tcp_sendmsg_locked_[k]
  [12] tcp_sendmsg_[k]
  [13] inet6_sendmsg_[k]
  [14] sock_sendmsg_[k]
  [15] __sys_sendto_[k]
  [16] __x64_sys_sendto_[k]
  [17] do_syscall_64_[k]
  [18] entry_SYSCALL_64_after_hwframe_[k]
  [19] __libc_send
  [20] one.nio.net.NativeSocket.write
  [21] one.nio.net.Session$ArrayQueueItem.write
  [22] one.nio.net.Session.write
  [23] one.nio.net.Session.write
  [24] one.nio.http.HttpSession.writeResponse
  [25] one.nio.http.HttpSession.sendResponse
  [26] RequestHandler2_status.handleRequest
  [27] one.nio.http.HttpServer.handleRequest
  [28] one.nio.http.HttpSession.handleParsedRequest
  [29] one.nio.http.HttpSession.processHttpBuffer
  [30] one.nio.http.HttpSession.processRead
  [31] one.nio.net.Session.process
  [32] one.nio.server.SelectorThread.run

--- 61081057 ns (0.89%), 6 samples
  [ 0] __ip_queue_xmit_[k]
  [ 1] ip_queue_xmit_[k]
  [ 2] __tcp_transmit_skb_[k]
  [ 3] tcp_write_xmit_[k]
  [ 4] __tcp_push_pending_frames_[k]
  [ 5] tcp_push_[k]
  [ 6] tcp_sendmsg_locked_[k]
  [ 7] tcp_sendmsg_[k]
  [ 8] inet6_sendmsg_[k]
  [ 9] sock_sendmsg_[k]
  [10] __sys_sendto_[k]
  [11] __x64_sys_sendto_[k]
  [12] do_syscall_64_[k]
  [13] entry_SYSCALL_64_after_hwframe_[k]
  [14] __libc_send
  [15] one.nio.net.NativeSocket.write
  [16] one.nio.net.Session$ArrayQueueItem.write
  [17] one.nio.net.Session.write
  [18] one.nio.net.Session.write
  [19] one.nio.http.HttpSession.writeResponse
  [20] one.nio.http.HttpSession.sendResponse
  [21] RequestHandler2_status.handleRequest
  [22] one.nio.http.HttpServer.handleRequest
  [23] one.nio.http.HttpSession.handleParsedRequest
  [24] one.nio.http.HttpSession.processHttpBuffer
  [25] one.nio.http.HttpSession.processRead
  [26] one.nio.net.Session.process
  [27] one.nio.server.SelectorThread.run

--- 51015028 ns (0.74%), 5 samples
  [ 0] __fget_[k]
  [ 1] __fget_light_[k]
  [ 2] __fdget_[k]
  [ 3] sockfd_lookup_light_[k]
  [ 4] __sys_recvfrom_[k]
  [ 5] __x64_sys_recvfrom_[k]
  [ 6] do_syscall_64_[k]
  [ 7] entry_SYSCALL_64_after_hwframe_[k]
  [ 8] __GI___recv
  [ 9] one.nio.net.NativeSocket.read
  [10] one.nio.net.Session.read
  [11] one.nio.http.HttpSession.processRead
  [12] one.nio.net.Session.process
  [13] one.nio.server.SelectorThread.run

--- 51006066 ns (0.74%), 5 samples
  [ 0] one.nio.net.NativeSelector.select
  [ 1] one.nio.server.SelectorThread.run

--- 50985463 ns (0.74%), 5 samples
  [ 0] ep_scan_ready_list.constprop.20_[k]
  [ 1] ep_poll_[k]
  [ 2] do_epoll_wait_[k]
  [ 3] __x64_sys_epoll_wait_[k]
  [ 4] do_syscall_64_[k]
  [ 5] entry_SYSCALL_64_after_hwframe_[k]
  [ 6] epoll_wait
  [ 7] [unknown]
  [ 8] one.nio.net.NativeSelector.epollWait
  [ 9] one.nio.net.NativeSelector.select
  [10] one.nio.server.SelectorThread.run

--- 50980892 ns (0.74%), 5 samples
  [ 0] one.nio.server.SelectorThread.run

--- 50962924 ns (0.74%), 5 samples
  [ 0] tcp_in_window?[nf_conntrack]_[k]
  [ 1] nf_conntrack_tcp_packet?[nf_conntrack]_[k]
  [ 2] nf_conntrack_in?[nf_conntrack]_[k]
  [ 3] ipv4_conntrack_local?[nf_conntrack]_[k]
  [ 4] nf_hook_slow_[k]
  [ 5] __ip_local_out_[k]
  [ 6] ip_local_out_[k]
  [ 7] __ip_queue_xmit_[k]
  [ 8] ip_queue_xmit_[k]
  [ 9] __tcp_transmit_skb_[k]
  [10] tcp_write_xmit_[k]
  [11] __tcp_push_pending_frames_[k]
  [12] tcp_push_[k]
  [13] tcp_sendmsg_locked_[k]
  [14] tcp_sendmsg_[k]
  [15] inet6_sendmsg_[k]
  [16] sock_sendmsg_[k]
  [17] __sys_sendto_[k]
  [18] __x64_sys_sendto_[k]
  [19] do_syscall_64_[k]
  [20] entry_SYSCALL_64_after_hwframe_[k]
  [21] __libc_send
  [22] one.nio.net.NativeSocket.write
  [23] one.nio.net.Session$ArrayQueueItem.write
  [24] one.nio.net.Session.write
  [25] one.nio.net.Session.write
  [26] one.nio.http.HttpSession.writeResponse
  [27] one.nio.http.HttpSession.sendResponse
  [28] RequestHandler2_status.handleRequest
  [29] one.nio.http.HttpServer.handleRequest
  [30] one.nio.http.HttpSession.handleParsedRequest
  [31] one.nio.http.HttpSession.processHttpBuffer
  [32] one.nio.http.HttpSession.processRead
  [33] one.nio.net.Session.process
  [34] one.nio.server.SelectorThread.run

--- 50943967 ns (0.74%), 5 samples
  [ 0] net_rx_action_[k]
  [ 1] __softirqentry_text_start_[k]
  [ 2] do_softirq_own_stack_[k]
  [ 3] do_softirq.part.20_[k]
  [ 4] __local_bh_enable_ip_[k]
  [ 5] ip_finish_output2_[k]
  [ 6] __ip_finish_output_[k]
  [ 7] ip_finish_output_[k]
  [ 8] ip_output_[k]
  [ 9] ip_local_out_[k]
  [10] __ip_queue_xmit_[k]
  [11] ip_queue_xmit_[k]
  [12] __tcp_transmit_skb_[k]
  [13] tcp_write_xmit_[k]
  [14] __tcp_push_pending_frames_[k]
  [15] tcp_push_[k]
  [16] tcp_sendmsg_locked_[k]
  [17] tcp_sendmsg_[k]
  [18] inet6_sendmsg_[k]
  [19] sock_sendmsg_[k]
  [20] __sys_sendto_[k]
  [21] __x64_sys_sendto_[k]
  [22] do_syscall_64_[k]
  [23] entry_SYSCALL_64_after_hwframe_[k]
  [24] __libc_send
  [25] one.nio.net.NativeSocket.write
  [26] one.nio.net.Session$ArrayQueueItem.write
  [27] one.nio.net.Session.write
  [28] one.nio.net.Session.write
  [29] one.nio.http.HttpSession.writeResponse
  [30] one.nio.http.HttpSession.sendResponse
  [31] RequestHandler2_status.handleRequest
  [32] one.nio.http.HttpServer.handleRequest
  [33] one.nio.http.HttpSession.handleParsedRequest
  [34] one.nio.http.HttpSession.processHttpBuffer
  [35] one.nio.http.HttpSession.processRead
  [36] one.nio.net.Session.process
  [37] one.nio.server.SelectorThread.run

--- 50924756 ns (0.74%), 5 samples
  [ 0] __check_object_size_[k]
  [ 1] simple_copy_to_iter_[k]
  [ 2] __skb_datagram_iter_[k]
  [ 3] skb_copy_datagram_iter_[k]
  [ 4] tcp_recvmsg_[k]
  [ 5] inet6_recvmsg_[k]
  [ 6] sock_recvmsg_[k]
  [ 7] __sys_recvfrom_[k]
  [ 8] __x64_sys_recvfrom_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __GI___recv
  [12] one.nio.net.NativeSocket.read
  [13] one.nio.net.Session.read
  [14] one.nio.http.HttpSession.processRead
  [15] one.nio.net.Session.process
  [16] one.nio.server.SelectorThread.run

--- 50912862 ns (0.74%), 5 samples
  [ 0] do_syscall_64_[k]
  [ 1] entry_SYSCALL_64_after_hwframe_[k]
  [ 2] __GI___recv
  [ 3] one.nio.net.NativeSocket.read
  [ 4] one.nio.net.Session.read
  [ 5] one.nio.http.HttpSession.processRead
  [ 6] one.nio.net.Session.process
  [ 7] one.nio.server.SelectorThread.run

--- 50860150 ns (0.74%), 5 samples
  [ 0] __libc_send
  [ 1] one.nio.net.NativeSocket.write
  [ 2] one.nio.net.Session$ArrayQueueItem.write
  [ 3] one.nio.net.Session.write
  [ 4] one.nio.net.Session.write
  [ 5] one.nio.http.HttpSession.writeResponse
  [ 6] one.nio.http.HttpSession.sendResponse
  [ 7] RequestHandler2_status.handleRequest
  [ 8] one.nio.http.HttpServer.handleRequest
  [ 9] one.nio.http.HttpSession.handleParsedRequest
  [10] one.nio.http.HttpSession.processHttpBuffer
  [11] one.nio.http.HttpSession.processRead
  [12] one.nio.net.Session.process
  [13] one.nio.server.SelectorThread.run

--- 50034633 ns (0.73%), 5 samples
  [ 0] SpinPause
  [ 1] G1ParEvacuateFollowersClosure::do_void()
  [ 2] G1ParTask::work(unsigned int)
  [ 3] GangWorker::loop()
  [ 4] Thread::call_run()
  [ 5] thread_native_entry(Thread*)
  [ 6] start_thread

--- 41573095 ns (0.60%), 4 samples
  [ 0] tcp_v4_rcv_[k]
  [ 1] ip_protocol_deliver_rcu_[k]
  [ 2] ip_local_deliver_finish_[k]
  [ 3] ip_local_deliver_[k]
  [ 4] ip_rcv_finish_[k]
  [ 5] ip_rcv_[k]
  [ 6] __netif_receive_skb_one_core_[k]
  [ 7] __netif_receive_skb_[k]
  [ 8] process_backlog_[k]
  [ 9] net_rx_action_[k]
  [10] __softirqentry_text_start_[k]
  [11] do_softirq_own_stack_[k]
  [12] do_softirq.part.20_[k]
  [13] __local_bh_enable_ip_[k]
  [14] ip_finish_output2_[k]
  [15] __ip_finish_output_[k]
  [16] ip_finish_output_[k]
  [17] ip_output_[k]
  [18] ip_local_out_[k]
  [19] __ip_queue_xmit_[k]
  [20] ip_queue_xmit_[k]
  [21] __tcp_transmit_skb_[k]
  [22] tcp_write_xmit_[k]
  [23] __tcp_push_pending_frames_[k]
  [24] tcp_push_[k]
  [25] tcp_sendmsg_locked_[k]
  [26] tcp_sendmsg_[k]
  [27] inet6_sendmsg_[k]
  [28] sock_sendmsg_[k]
  [29] __sys_sendto_[k]
  [30] __x64_sys_sendto_[k]
  [31] do_syscall_64_[k]
  [32] entry_SYSCALL_64_after_hwframe_[k]
  [33] __libc_send
  [34] one.nio.net.NativeSocket.write
  [35] one.nio.net.Session$ArrayQueueItem.write
  [36] one.nio.net.Session.write
  [37] one.nio.net.Session.write
  [38] one.nio.http.HttpSession.writeResponse
  [39] one.nio.http.HttpSession.sendResponse
  [40] RequestHandler2_status.handleRequest
  [41] one.nio.http.HttpServer.handleRequest
  [42] one.nio.http.HttpSession.handleParsedRequest
  [43] one.nio.http.HttpSession.processHttpBuffer
  [44] one.nio.http.HttpSession.processRead
  [45] one.nio.net.Session.process
  [46] one.nio.server.SelectorThread.run

--- 41101270 ns (0.60%), 4 samples
  [ 0] do_syscall_64_[k]
  [ 1] entry_SYSCALL_64_after_hwframe_[k]
  [ 2] epoll_wait
  [ 3] [unknown]
  [ 4] one.nio.net.NativeSelector.epollWait
  [ 5] one.nio.net.NativeSelector.select
  [ 6] one.nio.server.SelectorThread.run

--- 40796132 ns (0.59%), 4 samples
  [ 0] nf_conntrack_in?[nf_conntrack]_[k]
  [ 1] ipv4_conntrack_local?[nf_conntrack]_[k]
  [ 2] nf_hook_slow_[k]
  [ 3] __ip_local_out_[k]
  [ 4] ip_local_out_[k]
  [ 5] __ip_queue_xmit_[k]
  [ 6] ip_queue_xmit_[k]
  [ 7] __tcp_transmit_skb_[k]
  [ 8] tcp_write_xmit_[k]
  [ 9] __tcp_push_pending_frames_[k]
  [10] tcp_push_[k]
  [11] tcp_sendmsg_locked_[k]
  [12] tcp_sendmsg_[k]
  [13] inet6_sendmsg_[k]
  [14] sock_sendmsg_[k]
  [15] __sys_sendto_[k]
  [16] __x64_sys_sendto_[k]
  [17] do_syscall_64_[k]
  [18] entry_SYSCALL_64_after_hwframe_[k]
  [19] __libc_send
  [20] one.nio.net.NativeSocket.write
  [21] one.nio.net.Session$ArrayQueueItem.write
  [22] one.nio.net.Session.write
  [23] one.nio.net.Session.write
  [24] one.nio.http.HttpSession.writeResponse
  [25] one.nio.http.HttpSession.sendResponse
  [26] RequestHandler2_status.handleRequest
  [27] one.nio.http.HttpServer.handleRequest
  [28] one.nio.http.HttpSession.handleParsedRequest
  [29] one.nio.http.HttpSession.processHttpBuffer
  [30] one.nio.http.HttpSession.processRead
  [31] one.nio.net.Session.process
  [32] one.nio.server.SelectorThread.run

--- 40791206 ns (0.59%), 4 samples
  [ 0] eth_type_trans_[k]
  [ 1] loopback_xmit_[k]
  [ 2] dev_hard_start_xmit_[k]
  [ 3] __dev_queue_xmit_[k]
  [ 4] dev_queue_xmit_[k]
  [ 5] ip_finish_output2_[k]
  [ 6] __ip_finish_output_[k]
  [ 7] ip_finish_output_[k]
  [ 8] ip_output_[k]
  [ 9] ip_local_out_[k]
  [10] __ip_queue_xmit_[k]
  [11] ip_queue_xmit_[k]
  [12] __tcp_transmit_skb_[k]
  [13] tcp_write_xmit_[k]
  [14] __tcp_push_pending_frames_[k]
  [15] tcp_push_[k]
  [16] tcp_sendmsg_locked_[k]
  [17] tcp_sendmsg_[k]
  [18] inet6_sendmsg_[k]
  [19] sock_sendmsg_[k]
  [20] __sys_sendto_[k]
  [21] __x64_sys_sendto_[k]
  [22] do_syscall_64_[k]
  [23] entry_SYSCALL_64_after_hwframe_[k]
  [24] __libc_send
  [25] one.nio.net.NativeSocket.write
  [26] one.nio.net.Session$ArrayQueueItem.write
  [27] one.nio.net.Session.write
  [28] one.nio.net.Session.write
  [29] one.nio.http.HttpSession.writeResponse
  [30] one.nio.http.HttpSession.sendResponse
  [31] RequestHandler2_status.handleRequest
  [32] one.nio.http.HttpServer.handleRequest
  [33] one.nio.http.HttpSession.handleParsedRequest
  [34] one.nio.http.HttpSession.processHttpBuffer
  [35] one.nio.http.HttpSession.processRead
  [36] one.nio.net.Session.process
  [37] one.nio.server.SelectorThread.run

--- 40784069 ns (0.59%), 4 samples
  [ 0] jni_SetByteArrayRegion
  [ 1] Java_one_nio_net_NativeSocket_read
  [ 2] one.nio.net.NativeSocket.read
  [ 3] one.nio.net.Session.read
  [ 4] one.nio.http.HttpSession.processRead
  [ 5] one.nio.net.Session.process
  [ 6] one.nio.server.SelectorThread.run

--- 40773433 ns (0.59%), 4 samples
  [ 0] __netif_receive_skb_core_[k]
  [ 1] __netif_receive_skb_one_core_[k]
  [ 2] __netif_receive_skb_[k]
  [ 3] process_backlog_[k]
  [ 4] net_rx_action_[k]
  [ 5] __softirqentry_text_start_[k]
  [ 6] do_softirq_own_stack_[k]
  [ 7] do_softirq.part.20_[k]
  [ 8] __local_bh_enable_ip_[k]
  [ 9] ip_finish_output2_[k]
  [10] __ip_finish_output_[k]
  [11] ip_finish_output_[k]
  [12] ip_output_[k]
  [13] ip_local_out_[k]
  [14] __ip_queue_xmit_[k]
  [15] ip_queue_xmit_[k]
  [16] __tcp_transmit_skb_[k]
  [17] tcp_write_xmit_[k]
  [18] __tcp_push_pending_frames_[k]
  [19] tcp_push_[k]
  [20] tcp_sendmsg_locked_[k]
  [21] tcp_sendmsg_[k]
  [22] inet6_sendmsg_[k]
  [23] sock_sendmsg_[k]
  [24] __sys_sendto_[k]
  [25] __x64_sys_sendto_[k]
  [26] do_syscall_64_[k]
  [27] entry_SYSCALL_64_after_hwframe_[k]
  [28] __libc_send
  [29] one.nio.net.NativeSocket.write
  [30] one.nio.net.Session$ArrayQueueItem.write
  [31] one.nio.net.Session.write
  [32] one.nio.net.Session.write
  [33] one.nio.http.HttpSession.writeResponse
  [34] one.nio.http.HttpSession.sendResponse
  [35] RequestHandler2_status.handleRequest
  [36] one.nio.http.HttpServer.handleRequest
  [37] one.nio.http.HttpSession.handleParsedRequest
  [38] one.nio.http.HttpSession.processHttpBuffer
  [39] one.nio.http.HttpSession.processRead
  [40] one.nio.net.Session.process
  [41] one.nio.server.SelectorThread.run

--- 40766869 ns (0.59%), 4 samples
  [ 0] syscall_trace_enter_[k]
  [ 1] do_syscall_64_[k]
  [ 2] entry_SYSCALL_64_after_hwframe_[k]
  [ 3] __GI___recv
  [ 4] one.nio.net.NativeSocket.read
  [ 5] one.nio.net.Session.read
  [ 6] one.nio.http.HttpSession.processRead
  [ 7] one.nio.net.Session.process
  [ 8] one.nio.server.SelectorThread.run

--- 40756020 ns (0.59%), 4 samples
  [ 0] one.nio.http.Request.getHeader
  [ 1] one.nio.http.HttpSession.sendResponse
  [ 2] RequestHandler2_status.handleRequest
  [ 3] one.nio.http.HttpServer.handleRequest
  [ 4] one.nio.http.HttpSession.handleParsedRequest
  [ 5] one.nio.http.HttpSession.processHttpBuffer
  [ 6] one.nio.http.HttpSession.processRead
  [ 7] one.nio.net.Session.process
  [ 8] one.nio.server.SelectorThread.run

--- 40753982 ns (0.59%), 4 samples
  [ 0] one.nio.http.HttpSession.processHttpBuffer
  [ 1] one.nio.http.HttpSession.processRead
  [ 2] one.nio.net.Session.process
  [ 3] one.nio.server.SelectorThread.run

--- 40740321 ns (0.59%), 4 samples
  [ 0] __dev_queue_xmit_[k]
  [ 1] dev_queue_xmit_[k]
  [ 2] ip_finish_output2_[k]
  [ 3] __ip_finish_output_[k]
  [ 4] ip_finish_output_[k]
  [ 5] ip_output_[k]
  [ 6] ip_local_out_[k]
  [ 7] __ip_queue_xmit_[k]
  [ 8] ip_queue_xmit_[k]
  [ 9] __tcp_transmit_skb_[k]
  [10] tcp_write_xmit_[k]
  [11] __tcp_push_pending_frames_[k]
  [12] tcp_push_[k]
  [13] tcp_sendmsg_locked_[k]
  [14] tcp_sendmsg_[k]
  [15] inet6_sendmsg_[k]
  [16] sock_sendmsg_[k]
  [17] __sys_sendto_[k]
  [18] __x64_sys_sendto_[k]
  [19] do_syscall_64_[k]
  [20] entry_SYSCALL_64_after_hwframe_[k]
  [21] __libc_send
  [22] one.nio.net.NativeSocket.write
  [23] one.nio.net.Session$ArrayQueueItem.write
  [24] one.nio.net.Session.write
  [25] one.nio.net.Session.write
  [26] one.nio.http.HttpSession.writeResponse
  [27] one.nio.http.HttpSession.sendResponse
  [28] RequestHandler2_status.handleRequest
  [29] one.nio.http.HttpServer.handleRequest
  [30] one.nio.http.HttpSession.handleParsedRequest
  [31] one.nio.http.HttpSession.processHttpBuffer
  [32] one.nio.http.HttpSession.processRead
  [33] one.nio.net.Session.process
  [34] one.nio.server.SelectorThread.run

--- 40730708 ns (0.59%), 4 samples
  [ 0] ip_finish_output2_[k]
  [ 1] __ip_finish_output_[k]
  [ 2] ip_finish_output_[k]
  [ 3] ip_output_[k]
  [ 4] ip_local_out_[k]
  [ 5] __ip_queue_xmit_[k]
  [ 6] ip_queue_xmit_[k]
  [ 7] __tcp_transmit_skb_[k]
  [ 8] tcp_write_xmit_[k]
  [ 9] __tcp_push_pending_frames_[k]
  [10] tcp_push_[k]
  [11] tcp_sendmsg_locked_[k]
  [12] tcp_sendmsg_[k]
  [13] inet6_sendmsg_[k]
  [14] sock_sendmsg_[k]
  [15] __sys_sendto_[k]
  [16] __x64_sys_sendto_[k]
  [17] do_syscall_64_[k]
  [18] entry_SYSCALL_64_after_hwframe_[k]
  [19] __libc_send
  [20] one.nio.net.NativeSocket.write
  [21] one.nio.net.Session$ArrayQueueItem.write
  [22] one.nio.net.Session.write
  [23] one.nio.net.Session.write
  [24] one.nio.http.HttpSession.writeResponse
  [25] one.nio.http.HttpSession.sendResponse
  [26] RequestHandler2_status.handleRequest
  [27] one.nio.http.HttpServer.handleRequest
  [28] one.nio.http.HttpSession.handleParsedRequest
  [29] one.nio.http.HttpSession.processHttpBuffer
  [30] one.nio.http.HttpSession.processRead
  [31] one.nio.net.Session.process
  [32] one.nio.server.SelectorThread.run

--- 40722127 ns (0.59%), 4 samples
  [ 0] __slab_free_[k]
  [ 1] kfree_[k]
  [ 2] skb_free_head_[k]
  [ 3] skb_release_data_[k]
  [ 4] skb_release_all_[k]
  [ 5] __kfree_skb_[k]
  [ 6] tcp_clean_rtx_queue_[k]
  [ 7] tcp_ack_[k]
  [ 8] tcp_rcv_established_[k]
  [ 9] tcp_v4_do_rcv_[k]
  [10] tcp_v4_rcv_[k]
  [11] ip_protocol_deliver_rcu_[k]
  [12] ip_local_deliver_finish_[k]
  [13] ip_local_deliver_[k]
  [14] ip_rcv_finish_[k]
  [15] ip_rcv_[k]
  [16] __netif_receive_skb_one_core_[k]
  [17] __netif_receive_skb_[k]
  [18] process_backlog_[k]
  [19] net_rx_action_[k]
  [20] __softirqentry_text_start_[k]
  [21] do_softirq_own_stack_[k]
  [22] do_softirq.part.20_[k]
  [23] __local_bh_enable_ip_[k]
  [24] ip_finish_output2_[k]
  [25] __ip_finish_output_[k]
  [26] ip_finish_output_[k]
  [27] ip_output_[k]
  [28] ip_local_out_[k]
  [29] __ip_queue_xmit_[k]
  [30] ip_queue_xmit_[k]
  [31] __tcp_transmit_skb_[k]
  [32] tcp_write_xmit_[k]
  [33] __tcp_push_pending_frames_[k]
  [34] tcp_push_[k]
  [35] tcp_sendmsg_locked_[k]
  [36] tcp_sendmsg_[k]
  [37] inet6_sendmsg_[k]
  [38] sock_sendmsg_[k]
  [39] __sys_sendto_[k]
  [40] __x64_sys_sendto_[k]
  [41] do_syscall_64_[k]
  [42] entry_SYSCALL_64_after_hwframe_[k]
  [43] __libc_send
  [44] one.nio.net.NativeSocket.write
  [45] one.nio.net.Session$ArrayQueueItem.write
  [46] one.nio.net.Session.write
  [47] one.nio.net.Session.write
  [48] one.nio.http.HttpSession.writeResponse
  [49] one.nio.http.HttpSession.sendResponse
  [50] RequestHandler2_status.handleRequest
  [51] one.nio.http.HttpServer.handleRequest
  [52] one.nio.http.HttpSession.handleParsedRequest
  [53] one.nio.http.HttpSession.processHttpBuffer
  [54] one.nio.http.HttpSession.processRead
  [55] one.nio.net.Session.process
  [56] one.nio.server.SelectorThread.run

--- 40717725 ns (0.59%), 4 samples
  [ 0] ipt_do_table?[ip_tables]_[k]
  [ 1] iptable_filter_hook?[iptable_filter]_[k]
  [ 2] nf_hook_slow_[k]
  [ 3] ip_local_deliver_[k]
  [ 4] ip_rcv_finish_[k]
  [ 5] ip_rcv_[k]
  [ 6] __netif_receive_skb_one_core_[k]
  [ 7] __netif_receive_skb_[k]
  [ 8] process_backlog_[k]
  [ 9] net_rx_action_[k]
  [10] __softirqentry_text_start_[k]
  [11] do_softirq_own_stack_[k]
  [12] do_softirq.part.20_[k]
  [13] __local_bh_enable_ip_[k]
  [14] ip_finish_output2_[k]
  [15] __ip_finish_output_[k]
  [16] ip_finish_output_[k]
  [17] ip_output_[k]
  [18] ip_local_out_[k]
  [19] __ip_queue_xmit_[k]
  [20] ip_queue_xmit_[k]
  [21] __tcp_transmit_skb_[k]
  [22] tcp_write_xmit_[k]
  [23] __tcp_push_pending_frames_[k]
  [24] tcp_push_[k]
  [25] tcp_sendmsg_locked_[k]
  [26] tcp_sendmsg_[k]
  [27] inet6_sendmsg_[k]
  [28] sock_sendmsg_[k]
  [29] __sys_sendto_[k]
  [30] __x64_sys_sendto_[k]
  [31] do_syscall_64_[k]
  [32] entry_SYSCALL_64_after_hwframe_[k]
  [33] __libc_send
  [34] one.nio.net.NativeSocket.write
  [35] one.nio.net.Session$ArrayQueueItem.write
  [36] one.nio.net.Session.write
  [37] one.nio.net.Session.write
  [38] one.nio.http.HttpSession.writeResponse
  [39] one.nio.http.HttpSession.sendResponse
  [40] RequestHandler2_status.handleRequest
  [41] one.nio.http.HttpServer.handleRequest
  [42] one.nio.http.HttpSession.handleParsedRequest
  [43] one.nio.http.HttpSession.processHttpBuffer
  [44] one.nio.http.HttpSession.processRead
  [45] one.nio.net.Session.process
  [46] one.nio.server.SelectorThread.run

--- 40683118 ns (0.59%), 4 samples
  [ 0] _raw_spin_lock_bh_[k]
  [ 1] lock_sock_nested_[k]
  [ 2] tcp_recvmsg_[k]
  [ 3] inet6_recvmsg_[k]
  [ 4] sock_recvmsg_[k]
  [ 5] __sys_recvfrom_[k]
  [ 6] __x64_sys_recvfrom_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] __GI___recv
  [10] one.nio.net.NativeSocket.read
  [11] one.nio.net.Session.read
  [12] one.nio.http.HttpSession.processRead
  [13] one.nio.net.Session.process
  [14] one.nio.server.SelectorThread.run

--- 40680519 ns (0.59%), 4 samples
  [ 0] one.nio.http.Request.getHeader
  [ 1] one.nio.http.HttpSession.processHttpBuffer
  [ 2] one.nio.http.HttpSession.processRead
  [ 3] one.nio.net.Session.process
  [ 4] one.nio.server.SelectorThread.run

--- 30654064 ns (0.44%), 3 samples
  [ 0] __GI___recv
  [ 1] one.nio.net.NativeSocket.read
  [ 2] one.nio.net.Session.read
  [ 3] one.nio.http.HttpSession.processRead
  [ 4] one.nio.net.Session.process
  [ 5] one.nio.server.SelectorThread.run

--- 30624679 ns (0.44%), 3 samples
  [ 0] HandleMark::pop_and_restore()
  [ 1] jni_SetByteArrayRegion
  [ 2] Java_one_nio_net_NativeSocket_read
  [ 3] one.nio.net.NativeSocket.read
  [ 4] one.nio.net.Session.read
  [ 5] one.nio.http.HttpSession.processRead
  [ 6] one.nio.net.Session.process
  [ 7] one.nio.server.SelectorThread.run

--- 30621650 ns (0.44%), 3 samples
  [ 0] get_l4proto?[nf_conntrack]_[k]
  [ 1] nf_conntrack_in?[nf_conntrack]_[k]
  [ 2] ipv4_conntrack_local?[nf_conntrack]_[k]
  [ 3] nf_hook_slow_[k]
  [ 4] __ip_local_out_[k]
  [ 5] ip_local_out_[k]
  [ 6] __ip_queue_xmit_[k]
  [ 7] ip_queue_xmit_[k]
  [ 8] __tcp_transmit_skb_[k]
  [ 9] tcp_write_xmit_[k]
  [10] __tcp_push_pending_frames_[k]
  [11] tcp_push_[k]
  [12] tcp_sendmsg_locked_[k]
  [13] tcp_sendmsg_[k]
  [14] inet6_sendmsg_[k]
  [15] sock_sendmsg_[k]
  [16] __sys_sendto_[k]
  [17] __x64_sys_sendto_[k]
  [18] do_syscall_64_[k]
  [19] entry_SYSCALL_64_after_hwframe_[k]
  [20] __libc_send
  [21] one.nio.net.NativeSocket.write
  [22] one.nio.net.Session$ArrayQueueItem.write
  [23] one.nio.net.Session.write
  [24] one.nio.net.Session.write
  [25] one.nio.http.HttpSession.writeResponse
  [26] one.nio.http.HttpSession.sendResponse
  [27] RequestHandler2_status.handleRequest
  [28] one.nio.http.HttpServer.handleRequest
  [29] one.nio.http.HttpSession.handleParsedRequest
  [30] one.nio.http.HttpSession.processHttpBuffer
  [31] one.nio.http.HttpSession.processRead
  [32] one.nio.net.Session.process
  [33] one.nio.server.SelectorThread.run

--- 30620298 ns (0.44%), 3 samples
  [ 0] one.nio.net.Session.read
  [ 1] one.nio.http.HttpSession.processRead
  [ 2] one.nio.net.Session.process
  [ 3] one.nio.server.SelectorThread.run

--- 30617159 ns (0.44%), 3 samples
  [ 0] __ip_finish_output_[k]
  [ 1] ip_output_[k]
  [ 2] ip_local_out_[k]
  [ 3] __ip_queue_xmit_[k]
  [ 4] ip_queue_xmit_[k]
  [ 5] __tcp_transmit_skb_[k]
  [ 6] tcp_write_xmit_[k]
  [ 7] __tcp_push_pending_frames_[k]
  [ 8] tcp_push_[k]
  [ 9] tcp_sendmsg_locked_[k]
  [10] tcp_sendmsg_[k]
  [11] inet6_sendmsg_[k]
  [12] sock_sendmsg_[k]
  [13] __sys_sendto_[k]
  [14] __x64_sys_sendto_[k]
  [15] do_syscall_64_[k]
  [16] entry_SYSCALL_64_after_hwframe_[k]
  [17] __libc_send
  [18] one.nio.net.NativeSocket.write
  [19] one.nio.net.Session$ArrayQueueItem.write
  [20] one.nio.net.Session.write
  [21] one.nio.net.Session.write
  [22] one.nio.http.HttpSession.writeResponse
  [23] one.nio.http.HttpSession.sendResponse
  [24] RequestHandler2_status.handleRequest
  [25] one.nio.http.HttpServer.handleRequest
  [26] one.nio.http.HttpSession.handleParsedRequest
  [27] one.nio.http.HttpSession.processHttpBuffer
  [28] one.nio.http.HttpSession.processRead
  [29] one.nio.net.Session.process
  [30] one.nio.server.SelectorThread.run

--- 30613003 ns (0.44%), 3 samples
  [ 0] security_socket_recvmsg_[k]
  [ 1] sock_recvmsg_[k]
  [ 2] __sys_recvfrom_[k]
  [ 3] __x64_sys_recvfrom_[k]
  [ 4] do_syscall_64_[k]
  [ 5] entry_SYSCALL_64_after_hwframe_[k]
  [ 6] __GI___recv
  [ 7] one.nio.net.NativeSocket.read
  [ 8] one.nio.net.Session.read
  [ 9] one.nio.http.HttpSession.processRead
  [10] one.nio.net.Session.process
  [11] one.nio.server.SelectorThread.run

--- 30612239 ns (0.44%), 3 samples
  [ 0] ip_rcv_finish_core.isra.18_[k]
  [ 1] ip_rcv_finish_[k]
  [ 2] ip_rcv_[k]
  [ 3] __netif_receive_skb_one_core_[k]
  [ 4] __netif_receive_skb_[k]
  [ 5] process_backlog_[k]
  [ 6] net_rx_action_[k]
  [ 7] __softirqentry_text_start_[k]
  [ 8] do_softirq_own_stack_[k]
  [ 9] do_softirq.part.20_[k]
  [10] __local_bh_enable_ip_[k]
  [11] ip_finish_output2_[k]
  [12] __ip_finish_output_[k]
  [13] ip_finish_output_[k]
  [14] ip_output_[k]
  [15] ip_local_out_[k]
  [16] __ip_queue_xmit_[k]
  [17] ip_queue_xmit_[k]
  [18] __tcp_transmit_skb_[k]
  [19] tcp_write_xmit_[k]
  [20] __tcp_push_pending_frames_[k]
  [21] tcp_push_[k]
  [22] tcp_sendmsg_locked_[k]
  [23] tcp_sendmsg_[k]
  [24] inet6_sendmsg_[k]
  [25] sock_sendmsg_[k]
  [26] __sys_sendto_[k]
  [27] __x64_sys_sendto_[k]
  [28] do_syscall_64_[k]
  [29] entry_SYSCALL_64_after_hwframe_[k]
  [30] __libc_send
  [31] one.nio.net.NativeSocket.write
  [32] one.nio.net.Session$ArrayQueueItem.write
  [33] one.nio.net.Session.write
  [34] one.nio.net.Session.write
  [35] one.nio.http.HttpSession.writeResponse
  [36] one.nio.http.HttpSession.sendResponse
  [37] RequestHandler2_status.handleRequest
  [38] one.nio.http.HttpServer.handleRequest
  [39] one.nio.http.HttpSession.handleParsedRequest
  [40] one.nio.http.HttpSession.processHttpBuffer
  [41] one.nio.http.HttpSession.processRead
  [42] one.nio.net.Session.process
  [43] one.nio.server.SelectorThread.run

--- 30609433 ns (0.44%), 3 samples
  [ 0] __libc_disable_asynccancel
  [ 1] [unknown]
  [ 2] one.nio.net.NativeSelector.epollWait
  [ 3] one.nio.net.NativeSelector.select
  [ 4] one.nio.server.SelectorThread.run

--- 30602228 ns (0.44%), 3 samples
  [ 0] loopback_xmit_[k]
  [ 1] __dev_queue_xmit_[k]
  [ 2] dev_queue_xmit_[k]
  [ 3] ip_finish_output2_[k]
  [ 4] __ip_finish_output_[k]
  [ 5] ip_finish_output_[k]
  [ 6] ip_output_[k]
  [ 7] ip_local_out_[k]
  [ 8] __ip_queue_xmit_[k]
  [ 9] ip_queue_xmit_[k]
  [10] __tcp_transmit_skb_[k]
  [11] tcp_write_xmit_[k]
  [12] __tcp_push_pending_frames_[k]
  [13] tcp_push_[k]
  [14] tcp_sendmsg_locked_[k]
  [15] tcp_sendmsg_[k]
  [16] inet6_sendmsg_[k]
  [17] sock_sendmsg_[k]
  [18] __sys_sendto_[k]
  [19] __x64_sys_sendto_[k]
  [20] do_syscall_64_[k]
  [21] entry_SYSCALL_64_after_hwframe_[k]
  [22] __libc_send
  [23] one.nio.net.NativeSocket.write
  [24] one.nio.net.Session$ArrayQueueItem.write
  [25] one.nio.net.Session.write
  [26] one.nio.net.Session.write
  [27] one.nio.http.HttpSession.writeResponse
  [28] one.nio.http.HttpSession.sendResponse
  [29] RequestHandler2_status.handleRequest
  [30] one.nio.http.HttpServer.handleRequest
  [31] one.nio.http.HttpSession.handleParsedRequest
  [32] one.nio.http.HttpSession.processHttpBuffer
  [33] one.nio.http.HttpSession.processRead
  [34] one.nio.net.Session.process
  [35] one.nio.server.SelectorThread.run

--- 30600869 ns (0.44%), 3 samples
  [ 0] copy_user_generic_unrolled_[k]
  [ 1] _copy_from_iter_full_[k]
  [ 2] tcp_sendmsg_locked_[k]
  [ 3] tcp_sendmsg_[k]
  [ 4] inet6_sendmsg_[k]
  [ 5] sock_sendmsg_[k]
  [ 6] __sys_sendto_[k]
  [ 7] __x64_sys_sendto_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __libc_send
  [11] one.nio.net.NativeSocket.write
  [12] one.nio.net.Session$ArrayQueueItem.write
  [13] one.nio.net.Session.write
  [14] one.nio.net.Session.write
  [15] one.nio.http.HttpSession.writeResponse
  [16] one.nio.http.HttpSession.sendResponse
  [17] RequestHandler2_status.handleRequest
  [18] one.nio.http.HttpServer.handleRequest
  [19] one.nio.http.HttpSession.handleParsedRequest
  [20] one.nio.http.HttpSession.processHttpBuffer
  [21] one.nio.http.HttpSession.processRead
  [22] one.nio.net.Session.process
  [23] one.nio.server.SelectorThread.run

--- 30596256 ns (0.44%), 3 samples
  [ 0] fput_many_[k]
  [ 1] fput_[k]
  [ 2] __sys_sendto_[k]
  [ 3] __x64_sys_sendto_[k]
  [ 4] do_syscall_64_[k]
  [ 5] entry_SYSCALL_64_after_hwframe_[k]
  [ 6] __libc_send
  [ 7] one.nio.net.NativeSocket.write
  [ 8] one.nio.net.Session$ArrayQueueItem.write
  [ 9] one.nio.net.Session.write
  [10] one.nio.net.Session.write
  [11] one.nio.http.HttpSession.writeResponse
  [12] one.nio.http.HttpSession.sendResponse
  [13] RequestHandler2_status.handleRequest
  [14] one.nio.http.HttpServer.handleRequest
  [15] one.nio.http.HttpSession.handleParsedRequest
  [16] one.nio.http.HttpSession.processHttpBuffer
  [17] one.nio.http.HttpSession.processRead
  [18] one.nio.net.Session.process
  [19] one.nio.server.SelectorThread.run

--- 30595220 ns (0.44%), 3 samples
  [ 0] sock_def_readable_[k]
  [ 1] tcp_rcv_established_[k]
  [ 2] tcp_v4_do_rcv_[k]
  [ 3] tcp_v4_rcv_[k]
  [ 4] ip_protocol_deliver_rcu_[k]
  [ 5] ip_local_deliver_finish_[k]
  [ 6] ip_local_deliver_[k]
  [ 7] ip_rcv_finish_[k]
  [ 8] ip_rcv_[k]
  [ 9] __netif_receive_skb_one_core_[k]
  [10] __netif_receive_skb_[k]
  [11] process_backlog_[k]
  [12] net_rx_action_[k]
  [13] __softirqentry_text_start_[k]
  [14] do_softirq_own_stack_[k]
  [15] do_softirq.part.20_[k]
  [16] __local_bh_enable_ip_[k]
  [17] ip_finish_output2_[k]
  [18] __ip_finish_output_[k]
  [19] ip_finish_output_[k]
  [20] ip_output_[k]
  [21] ip_local_out_[k]
  [22] __ip_queue_xmit_[k]
  [23] ip_queue_xmit_[k]
  [24] __tcp_transmit_skb_[k]
  [25] tcp_write_xmit_[k]
  [26] __tcp_push_pending_frames_[k]
  [27] tcp_push_[k]
  [28] tcp_sendmsg_locked_[k]
  [29] tcp_sendmsg_[k]
  [30] inet6_sendmsg_[k]
  [31] sock_sendmsg_[k]
  [32] __sys_sendto_[k]
  [33] __x64_sys_sendto_[k]
  [34] do_syscall_64_[k]
  [35] entry_SYSCALL_64_after_hwframe_[k]
  [36] __libc_send
  [37] one.nio.net.NativeSocket.write
  [38] one.nio.net.Session$ArrayQueueItem.write
  [39] one.nio.net.Session.write
  [40] one.nio.net.Session.write
  [41] one.nio.http.HttpSession.writeResponse
  [42] one.nio.http.HttpSession.sendResponse
  [43] RequestHandler2_status.handleRequest
  [44] one.nio.http.HttpServer.handleRequest
  [45] one.nio.http.HttpSession.handleParsedRequest
  [46] one.nio.http.HttpSession.processHttpBuffer
  [47] one.nio.http.HttpSession.processRead
  [48] one.nio.net.Session.process
  [49] one.nio.server.SelectorThread.run

--- 30594485 ns (0.44%), 3 samples
  [ 0] __tcp_select_window_[k]
  [ 1] tcp_recvmsg_[k]
  [ 2] inet6_recvmsg_[k]
  [ 3] sock_recvmsg_[k]
  [ 4] __sys_recvfrom_[k]
  [ 5] __x64_sys_recvfrom_[k]
  [ 6] do_syscall_64_[k]
  [ 7] entry_SYSCALL_64_after_hwframe_[k]
  [ 8] __GI___recv
  [ 9] one.nio.net.NativeSocket.read
  [10] one.nio.net.Session.read
  [11] one.nio.http.HttpSession.processRead
  [12] one.nio.net.Session.process
  [13] one.nio.server.SelectorThread.run

--- 30593766 ns (0.44%), 3 samples
  [ 0] tcp_rcv_established_[k]
  [ 1] tcp_v4_do_rcv_[k]
  [ 2] tcp_v4_rcv_[k]
  [ 3] ip_protocol_deliver_rcu_[k]
  [ 4] ip_local_deliver_finish_[k]
  [ 5] ip_local_deliver_[k]
  [ 6] ip_rcv_finish_[k]
  [ 7] ip_rcv_[k]
  [ 8] __netif_receive_skb_one_core_[k]
  [ 9] __netif_receive_skb_[k]
  [10] process_backlog_[k]
  [11] net_rx_action_[k]
  [12] __softirqentry_text_start_[k]
  [13] do_softirq_own_stack_[k]
  [14] do_softirq.part.20_[k]
  [15] __local_bh_enable_ip_[k]
  [16] ip_finish_output2_[k]
  [17] __ip_finish_output_[k]
  [18] ip_finish_output_[k]
  [19] ip_output_[k]
  [20] ip_local_out_[k]
  [21] __ip_queue_xmit_[k]
  [22] ip_queue_xmit_[k]
  [23] __tcp_transmit_skb_[k]
  [24] tcp_write_xmit_[k]
  [25] __tcp_push_pending_frames_[k]
  [26] tcp_push_[k]
  [27] tcp_sendmsg_locked_[k]
  [28] tcp_sendmsg_[k]
  [29] inet6_sendmsg_[k]
  [30] sock_sendmsg_[k]
  [31] __sys_sendto_[k]
  [32] __x64_sys_sendto_[k]
  [33] do_syscall_64_[k]
  [34] entry_SYSCALL_64_after_hwframe_[k]
  [35] __libc_send
  [36] one.nio.net.NativeSocket.write
  [37] one.nio.net.Session$ArrayQueueItem.write
  [38] one.nio.net.Session.write
  [39] one.nio.net.Session.write
  [40] one.nio.http.HttpSession.writeResponse
  [41] one.nio.http.HttpSession.sendResponse
  [42] RequestHandler2_status.handleRequest
  [43] one.nio.http.HttpServer.handleRequest
  [44] one.nio.http.HttpSession.handleParsedRequest
  [45] one.nio.http.HttpSession.processHttpBuffer
  [46] one.nio.http.HttpSession.processRead
  [47] one.nio.net.Session.process
  [48] one.nio.server.SelectorThread.run

--- 30589203 ns (0.44%), 3 samples
  [ 0] tcp_v4_fill_cb_[k]
  [ 1] tcp_v4_rcv_[k]
  [ 2] ip_protocol_deliver_rcu_[k]
  [ 3] ip_local_deliver_finish_[k]
  [ 4] ip_local_deliver_[k]
  [ 5] ip_rcv_finish_[k]
  [ 6] ip_rcv_[k]
  [ 7] __netif_receive_skb_one_core_[k]
  [ 8] __netif_receive_skb_[k]
  [ 9] process_backlog_[k]
  [10] net_rx_action_[k]
  [11] __softirqentry_text_start_[k]
  [12] do_softirq_own_stack_[k]
  [13] do_softirq.part.20_[k]
  [14] __local_bh_enable_ip_[k]
  [15] ip_finish_output2_[k]
  [16] __ip_finish_output_[k]
  [17] ip_finish_output_[k]
  [18] ip_output_[k]
  [19] ip_local_out_[k]
  [20] __ip_queue_xmit_[k]
  [21] ip_queue_xmit_[k]
  [22] __tcp_transmit_skb_[k]
  [23] tcp_write_xmit_[k]
  [24] __tcp_push_pending_frames_[k]
  [25] tcp_push_[k]
  [26] tcp_sendmsg_locked_[k]
  [27] tcp_sendmsg_[k]
  [28] inet6_sendmsg_[k]
  [29] sock_sendmsg_[k]
  [30] __sys_sendto_[k]
  [31] __x64_sys_sendto_[k]
  [32] do_syscall_64_[k]
  [33] entry_SYSCALL_64_after_hwframe_[k]
  [34] __libc_send
  [35] one.nio.net.NativeSocket.write
  [36] one.nio.net.Session$ArrayQueueItem.write
  [37] one.nio.net.Session.write
  [38] one.nio.net.Session.write
  [39] one.nio.http.HttpSession.writeResponse
  [40] one.nio.http.HttpSession.sendResponse
  [41] RequestHandler2_status.handleRequest
  [42] one.nio.http.HttpServer.handleRequest
  [43] one.nio.http.HttpSession.handleParsedRequest
  [44] one.nio.http.HttpSession.processHttpBuffer
  [45] one.nio.http.HttpSession.processRead
  [46] one.nio.net.Session.process
  [47] one.nio.server.SelectorThread.run

--- 30583915 ns (0.44%), 3 samples
  [ 0] tcp_clean_rtx_queue_[k]
  [ 1] tcp_ack_[k]
  [ 2] tcp_rcv_established_[k]
  [ 3] tcp_v4_do_rcv_[k]
  [ 4] tcp_v4_rcv_[k]
  [ 5] ip_protocol_deliver_rcu_[k]
  [ 6] ip_local_deliver_finish_[k]
  [ 7] ip_local_deliver_[k]
  [ 8] ip_rcv_finish_[k]
  [ 9] ip_rcv_[k]
  [10] __netif_receive_skb_one_core_[k]
  [11] __netif_receive_skb_[k]
  [12] process_backlog_[k]
  [13] net_rx_action_[k]
  [14] __softirqentry_text_start_[k]
  [15] do_softirq_own_stack_[k]
  [16] do_softirq.part.20_[k]
  [17] __local_bh_enable_ip_[k]
  [18] ip_finish_output2_[k]
  [19] __ip_finish_output_[k]
  [20] ip_finish_output_[k]
  [21] ip_output_[k]
  [22] ip_local_out_[k]
  [23] __ip_queue_xmit_[k]
  [24] ip_queue_xmit_[k]
  [25] __tcp_transmit_skb_[k]
  [26] tcp_write_xmit_[k]
  [27] __tcp_push_pending_frames_[k]
  [28] tcp_push_[k]
  [29] tcp_sendmsg_locked_[k]
  [30] tcp_sendmsg_[k]
  [31] inet6_sendmsg_[k]
  [32] sock_sendmsg_[k]
  [33] __sys_sendto_[k]
  [34] __x64_sys_sendto_[k]
  [35] do_syscall_64_[k]
  [36] entry_SYSCALL_64_after_hwframe_[k]
  [37] __libc_send
  [38] one.nio.net.NativeSocket.write
  [39] one.nio.net.Session$ArrayQueueItem.write
  [40] one.nio.net.Session.write
  [41] one.nio.net.Session.write
  [42] one.nio.http.HttpSession.writeResponse
  [43] one.nio.http.HttpSession.sendResponse
  [44] RequestHandler2_status.handleRequest
  [45] one.nio.http.HttpServer.handleRequest
  [46] one.nio.http.HttpSession.handleParsedRequest
  [47] one.nio.http.HttpSession.processHttpBuffer
  [48] one.nio.http.HttpSession.processRead
  [49] one.nio.net.Session.process
  [50] one.nio.server.SelectorThread.run

--- 30583789 ns (0.44%), 3 samples
  [ 0] tcp_wfree_[k]
  [ 1] loopback_xmit_[k]
  [ 2] dev_hard_start_xmit_[k]
  [ 3] __dev_queue_xmit_[k]
  [ 4] dev_queue_xmit_[k]
  [ 5] ip_finish_output2_[k]
  [ 6] __ip_finish_output_[k]
  [ 7] ip_finish_output_[k]
  [ 8] ip_output_[k]
  [ 9] ip_local_out_[k]
  [10] __ip_queue_xmit_[k]
  [11] ip_queue_xmit_[k]
  [12] __tcp_transmit_skb_[k]
  [13] tcp_write_xmit_[k]
  [14] __tcp_push_pending_frames_[k]
  [15] tcp_push_[k]
  [16] tcp_sendmsg_locked_[k]
  [17] tcp_sendmsg_[k]
  [18] inet6_sendmsg_[k]
  [19] sock_sendmsg_[k]
  [20] __sys_sendto_[k]
  [21] __x64_sys_sendto_[k]
  [22] do_syscall_64_[k]
  [23] entry_SYSCALL_64_after_hwframe_[k]
  [24] __libc_send
  [25] one.nio.net.NativeSocket.write
  [26] one.nio.net.Session$ArrayQueueItem.write
  [27] one.nio.net.Session.write
  [28] one.nio.net.Session.write
  [29] one.nio.http.HttpSession.writeResponse
  [30] one.nio.http.HttpSession.sendResponse
  [31] RequestHandler2_status.handleRequest
  [32] one.nio.http.HttpServer.handleRequest
  [33] one.nio.http.HttpSession.handleParsedRequest
  [34] one.nio.http.HttpSession.processHttpBuffer
  [35] one.nio.http.HttpSession.processRead
  [36] one.nio.net.Session.process
  [37] one.nio.server.SelectorThread.run

--- 30577330 ns (0.44%), 3 samples
  [ 0] aa_label_sk_perm.part.4_[k]
  [ 1] aa_sk_perm_[k]
  [ 2] aa_sock_msg_perm_[k]
  [ 3] apparmor_socket_recvmsg_[k]
  [ 4] security_socket_recvmsg_[k]
  [ 5] sock_recvmsg_[k]
  [ 6] __sys_recvfrom_[k]
  [ 7] __x64_sys_recvfrom_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __GI___recv
  [11] one.nio.net.NativeSocket.read
  [12] one.nio.net.Session.read
  [13] one.nio.http.HttpSession.processRead
  [14] one.nio.net.Session.process
  [15] one.nio.server.SelectorThread.run

--- 30576982 ns (0.44%), 3 samples
  [ 0] mod_timer_[k]
  [ 1] sk_reset_timer_[k]
  [ 2] tcp_rearm_rto_[k]
  [ 3] tcp_event_new_data_sent_[k]
  [ 4] tcp_write_xmit_[k]
  [ 5] __tcp_push_pending_frames_[k]
  [ 6] tcp_push_[k]
  [ 7] tcp_sendmsg_locked_[k]
  [ 8] tcp_sendmsg_[k]
  [ 9] inet6_sendmsg_[k]
  [10] sock_sendmsg_[k]
  [11] __sys_sendto_[k]
  [12] __x64_sys_sendto_[k]
  [13] do_syscall_64_[k]
  [14] entry_SYSCALL_64_after_hwframe_[k]
  [15] __libc_send
  [16] one.nio.net.NativeSocket.write
  [17] one.nio.net.Session$ArrayQueueItem.write
  [18] one.nio.net.Session.write
  [19] one.nio.net.Session.write
  [20] one.nio.http.HttpSession.writeResponse
  [21] one.nio.http.HttpSession.sendResponse
  [22] RequestHandler2_status.handleRequest
  [23] one.nio.http.HttpServer.handleRequest
  [24] one.nio.http.HttpSession.handleParsedRequest
  [25] one.nio.http.HttpSession.processHttpBuffer
  [26] one.nio.http.HttpSession.processRead
  [27] one.nio.net.Session.process
  [28] one.nio.server.SelectorThread.run

--- 30576752 ns (0.44%), 3 samples
  [ 0] nf_ct_get_tuple?[nf_conntrack]_[k]
  [ 1] ipv4_conntrack_local?[nf_conntrack]_[k]
  [ 2] nf_hook_slow_[k]
  [ 3] __ip_local_out_[k]
  [ 4] ip_local_out_[k]
  [ 5] __ip_queue_xmit_[k]
  [ 6] ip_queue_xmit_[k]
  [ 7] __tcp_transmit_skb_[k]
  [ 8] tcp_write_xmit_[k]
  [ 9] __tcp_push_pending_frames_[k]
  [10] tcp_push_[k]
  [11] tcp_sendmsg_locked_[k]
  [12] tcp_sendmsg_[k]
  [13] inet6_sendmsg_[k]
  [14] sock_sendmsg_[k]
  [15] __sys_sendto_[k]
  [16] __x64_sys_sendto_[k]
  [17] do_syscall_64_[k]
  [18] entry_SYSCALL_64_after_hwframe_[k]
  [19] __libc_send
  [20] one.nio.net.NativeSocket.write
  [21] one.nio.net.Session$ArrayQueueItem.write
  [22] one.nio.net.Session.write
  [23] one.nio.net.Session.write
  [24] one.nio.http.HttpSession.writeResponse
  [25] one.nio.http.HttpSession.sendResponse
  [26] RequestHandler2_status.handleRequest
  [27] one.nio.http.HttpServer.handleRequest
  [28] one.nio.http.HttpSession.handleParsedRequest
  [29] one.nio.http.HttpSession.processHttpBuffer
  [30] one.nio.http.HttpSession.processRead
  [31] one.nio.net.Session.process
  [32] one.nio.server.SelectorThread.run

--- 30575887 ns (0.44%), 3 samples
  [ 0] ep_poll_[k]
  [ 1] do_epoll_wait_[k]
  [ 2] __x64_sys_epoll_wait_[k]
  [ 3] do_syscall_64_[k]
  [ 4] entry_SYSCALL_64_after_hwframe_[k]
  [ 5] epoll_wait
  [ 6] [unknown]
  [ 7] one.nio.net.NativeSelector.epollWait
  [ 8] one.nio.net.NativeSelector.select
  [ 9] one.nio.server.SelectorThread.run

--- 30574450 ns (0.44%), 3 samples
  [ 0] unroll_tree_refs_[k]
  [ 1] __audit_syscall_exit_[k]
  [ 2] syscall_slow_exit_work_[k]
  [ 3] do_syscall_64_[k]
  [ 4] entry_SYSCALL_64_after_hwframe_[k]
  [ 5] __libc_send
  [ 6] one.nio.net.NativeSocket.write
  [ 7] one.nio.net.Session$ArrayQueueItem.write
  [ 8] one.nio.net.Session.write
  [ 9] one.nio.net.Session.write
  [10] one.nio.http.HttpSession.writeResponse
  [11] one.nio.http.HttpSession.sendResponse
  [12] RequestHandler2_status.handleRequest
  [13] one.nio.http.HttpServer.handleRequest
  [14] one.nio.http.HttpSession.handleParsedRequest
  [15] one.nio.http.HttpSession.processHttpBuffer
  [16] one.nio.http.HttpSession.processRead
  [17] one.nio.net.Session.process
  [18] one.nio.server.SelectorThread.run

--- 30568303 ns (0.44%), 3 samples
  [ 0] jni_SetByteArrayRegion
  [ 1] one.nio.net.NativeSocket.read
  [ 2] one.nio.net.Session.read
  [ 3] one.nio.http.HttpSession.processRead
  [ 4] one.nio.net.Session.process
  [ 5] one.nio.server.SelectorThread.run

--- 30562293 ns (0.44%), 3 samples
  [ 0] ip_output_[k]
  [ 1] ip_local_out_[k]
  [ 2] __ip_queue_xmit_[k]
  [ 3] ip_queue_xmit_[k]
  [ 4] __tcp_transmit_skb_[k]
  [ 5] tcp_write_xmit_[k]
  [ 6] __tcp_push_pending_frames_[k]
  [ 7] tcp_push_[k]
  [ 8] tcp_sendmsg_locked_[k]
  [ 9] tcp_sendmsg_[k]
  [10] inet6_sendmsg_[k]
  [11] sock_sendmsg_[k]
  [12] __sys_sendto_[k]
  [13] __x64_sys_sendto_[k]
  [14] do_syscall_64_[k]
  [15] entry_SYSCALL_64_after_hwframe_[k]
  [16] __libc_send
  [17] one.nio.net.NativeSocket.write
  [18] one.nio.net.Session$ArrayQueueItem.write
  [19] one.nio.net.Session.write
  [20] one.nio.net.Session.write
  [21] one.nio.http.HttpSession.writeResponse
  [22] one.nio.http.HttpSession.sendResponse
  [23] RequestHandler2_status.handleRequest
  [24] one.nio.http.HttpServer.handleRequest
  [25] one.nio.http.HttpSession.handleParsedRequest
  [26] one.nio.http.HttpSession.processHttpBuffer
  [27] one.nio.http.HttpSession.processRead
  [28] one.nio.net.Session.process
  [29] one.nio.server.SelectorThread.run

--- 30556986 ns (0.44%), 3 samples
  [ 0] import_single_range_[k]
  [ 1] __x64_sys_recvfrom_[k]
  [ 2] do_syscall_64_[k]
  [ 3] entry_SYSCALL_64_after_hwframe_[k]
  [ 4] __GI___recv
  [ 5] one.nio.net.NativeSocket.read
  [ 6] one.nio.net.Session.read
  [ 7] one.nio.http.HttpSession.processRead
  [ 8] one.nio.net.Session.process
  [ 9] one.nio.server.SelectorThread.run

--- 30555057 ns (0.44%), 3 samples
  [ 0] enqueue_to_backlog_[k]
  [ 1] netif_rx_internal_[k]
  [ 2] netif_rx_[k]
  [ 3] loopback_xmit_[k]
  [ 4] dev_hard_start_xmit_[k]
  [ 5] __dev_queue_xmit_[k]
  [ 6] dev_queue_xmit_[k]
  [ 7] ip_finish_output2_[k]
  [ 8] __ip_finish_output_[k]
  [ 9] ip_finish_output_[k]
  [10] ip_output_[k]
  [11] ip_local_out_[k]
  [12] __ip_queue_xmit_[k]
  [13] ip_queue_xmit_[k]
  [14] __tcp_transmit_skb_[k]
  [15] tcp_write_xmit_[k]
  [16] __tcp_push_pending_frames_[k]
  [17] tcp_push_[k]
  [18] tcp_sendmsg_locked_[k]
  [19] tcp_sendmsg_[k]
  [20] inet6_sendmsg_[k]
  [21] sock_sendmsg_[k]
  [22] __sys_sendto_[k]
  [23] __x64_sys_sendto_[k]
  [24] do_syscall_64_[k]
  [25] entry_SYSCALL_64_after_hwframe_[k]
  [26] __libc_send
  [27] one.nio.net.NativeSocket.write
  [28] one.nio.net.Session$ArrayQueueItem.write
  [29] one.nio.net.Session.write
  [30] one.nio.net.Session.write
  [31] one.nio.http.HttpSession.writeResponse
  [32] one.nio.http.HttpSession.sendResponse
  [33] RequestHandler2_status.handleRequest
  [34] one.nio.http.HttpServer.handleRequest
  [35] one.nio.http.HttpSession.handleParsedRequest
  [36] one.nio.http.HttpSession.processHttpBuffer
  [37] one.nio.http.HttpSession.processRead
  [38] one.nio.net.Session.process
  [39] one.nio.server.SelectorThread.run

--- 30554480 ns (0.44%), 3 samples
  [ 0] one.nio.net.NativeSelector.epollWait
  [ 1] one.nio.net.Session.process
  [ 2] one.nio.server.SelectorThread.run

--- 30548822 ns (0.44%), 3 samples
  [ 0] one.nio.net.NativeSocket.read
  [ 1] one.nio.net.Session.read
  [ 2] one.nio.http.HttpSession.processRead
  [ 3] one.nio.net.Session.process
  [ 4] one.nio.server.SelectorThread.run

--- 30544799 ns (0.44%), 3 samples
  [ 0] skb_release_data_[k]
  [ 1] skb_release_all_[k]
  [ 2] __kfree_skb_[k]
  [ 3] tcp_clean_rtx_queue_[k]
  [ 4] tcp_ack_[k]
  [ 5] tcp_rcv_established_[k]
  [ 6] tcp_v4_do_rcv_[k]
  [ 7] tcp_v4_rcv_[k]
  [ 8] ip_protocol_deliver_rcu_[k]
  [ 9] ip_local_deliver_finish_[k]
  [10] ip_local_deliver_[k]
  [11] ip_rcv_finish_[k]
  [12] ip_rcv_[k]
  [13] __netif_receive_skb_one_core_[k]
  [14] __netif_receive_skb_[k]
  [15] process_backlog_[k]
  [16] net_rx_action_[k]
  [17] __softirqentry_text_start_[k]
  [18] do_softirq_own_stack_[k]
  [19] do_softirq.part.20_[k]
  [20] __local_bh_enable_ip_[k]
  [21] ip_finish_output2_[k]
  [22] __ip_finish_output_[k]
  [23] ip_finish_output_[k]
  [24] ip_output_[k]
  [25] ip_local_out_[k]
  [26] __ip_queue_xmit_[k]
  [27] ip_queue_xmit_[k]
  [28] __tcp_transmit_skb_[k]
  [29] tcp_write_xmit_[k]
  [30] __tcp_push_pending_frames_[k]
  [31] tcp_push_[k]
  [32] tcp_sendmsg_locked_[k]
  [33] tcp_sendmsg_[k]
  [34] inet6_sendmsg_[k]
  [35] sock_sendmsg_[k]
  [36] __sys_sendto_[k]
  [37] __x64_sys_sendto_[k]
  [38] do_syscall_64_[k]
  [39] entry_SYSCALL_64_after_hwframe_[k]
  [40] __libc_send
  [41] one.nio.net.NativeSocket.write
  [42] one.nio.net.Session$ArrayQueueItem.write
  [43] one.nio.net.Session.write
  [44] one.nio.net.Session.write
  [45] one.nio.http.HttpSession.writeResponse
  [46] one.nio.http.HttpSession.sendResponse
  [47] RequestHandler2_status.handleRequest
  [48] one.nio.http.HttpServer.handleRequest
  [49] one.nio.http.HttpSession.handleParsedRequest
  [50] one.nio.http.HttpSession.processHttpBuffer
  [51] one.nio.http.HttpSession.processRead
  [52] one.nio.net.Session.process
  [53] one.nio.server.SelectorThread.run

--- 30539972 ns (0.44%), 3 samples
  [ 0] skb_page_frag_refill_[k]
  [ 1] sk_page_frag_refill_[k]
  [ 2] tcp_sendmsg_locked_[k]
  [ 3] tcp_sendmsg_[k]
  [ 4] inet6_sendmsg_[k]
  [ 5] sock_sendmsg_[k]
  [ 6] __sys_sendto_[k]
  [ 7] __x64_sys_sendto_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __libc_send
  [11] one.nio.net.NativeSocket.write
  [12] one.nio.net.Session$ArrayQueueItem.write
  [13] one.nio.net.Session.write
  [14] one.nio.net.Session.write
  [15] one.nio.http.HttpSession.writeResponse
  [16] one.nio.http.HttpSession.sendResponse
  [17] RequestHandler2_status.handleRequest
  [18] one.nio.http.HttpServer.handleRequest
  [19] one.nio.http.HttpSession.handleParsedRequest
  [20] one.nio.http.HttpSession.processHttpBuffer
  [21] one.nio.http.HttpSession.processRead
  [22] one.nio.net.Session.process
  [23] one.nio.server.SelectorThread.run

--- 30536377 ns (0.44%), 3 samples
  [ 0] _raw_spin_lock_[k]
  [ 1] tcp_v4_rcv_[k]
  [ 2] ip_protocol_deliver_rcu_[k]
  [ 3] ip_local_deliver_finish_[k]
  [ 4] ip_local_deliver_[k]
  [ 5] ip_rcv_finish_[k]
  [ 6] ip_rcv_[k]
  [ 7] __netif_receive_skb_one_core_[k]
  [ 8] __netif_receive_skb_[k]
  [ 9] process_backlog_[k]
  [10] net_rx_action_[k]
  [11] __softirqentry_text_start_[k]
  [12] do_softirq_own_stack_[k]
  [13] do_softirq.part.20_[k]
  [14] __local_bh_enable_ip_[k]
  [15] ip_finish_output2_[k]
  [16] __ip_finish_output_[k]
  [17] ip_finish_output_[k]
  [18] ip_output_[k]
  [19] ip_local_out_[k]
  [20] __ip_queue_xmit_[k]
  [21] ip_queue_xmit_[k]
  [22] __tcp_transmit_skb_[k]
  [23] tcp_write_xmit_[k]
  [24] __tcp_push_pending_frames_[k]
  [25] tcp_push_[k]
  [26] tcp_sendmsg_locked_[k]
  [27] tcp_sendmsg_[k]
  [28] inet6_sendmsg_[k]
  [29] sock_sendmsg_[k]
  [30] __sys_sendto_[k]
  [31] __x64_sys_sendto_[k]
  [32] do_syscall_64_[k]
  [33] entry_SYSCALL_64_after_hwframe_[k]
  [34] __libc_send
  [35] one.nio.net.NativeSocket.write
  [36] one.nio.net.Session$ArrayQueueItem.write
  [37] one.nio.net.Session.write
  [38] one.nio.net.Session.write
  [39] one.nio.http.HttpSession.writeResponse
  [40] one.nio.http.HttpSession.sendResponse
  [41] RequestHandler2_status.handleRequest
  [42] one.nio.http.HttpServer.handleRequest
  [43] one.nio.http.HttpSession.handleParsedRequest
  [44] one.nio.http.HttpSession.processHttpBuffer
  [45] one.nio.http.HttpSession.processRead
  [46] one.nio.net.Session.process
  [47] one.nio.server.SelectorThread.run

--- 30534725 ns (0.44%), 3 samples
  [ 0] __slab_free_[k]
  [ 1] kmem_cache_free_[k]
  [ 2] kfree_skbmem_[k]
  [ 3] __kfree_skb_[k]
  [ 4] tcp_clean_rtx_queue_[k]
  [ 5] tcp_ack_[k]
  [ 6] tcp_rcv_established_[k]
  [ 7] tcp_v4_do_rcv_[k]
  [ 8] tcp_v4_rcv_[k]
  [ 9] ip_protocol_deliver_rcu_[k]
  [10] ip_local_deliver_finish_[k]
  [11] ip_local_deliver_[k]
  [12] ip_rcv_finish_[k]
  [13] ip_rcv_[k]
  [14] __netif_receive_skb_one_core_[k]
  [15] __netif_receive_skb_[k]
  [16] process_backlog_[k]
  [17] net_rx_action_[k]
  [18] __softirqentry_text_start_[k]
  [19] do_softirq_own_stack_[k]
  [20] do_softirq.part.20_[k]
  [21] __local_bh_enable_ip_[k]
  [22] ip_finish_output2_[k]
  [23] __ip_finish_output_[k]
  [24] ip_finish_output_[k]
  [25] ip_output_[k]
  [26] ip_local_out_[k]
  [27] __ip_queue_xmit_[k]
  [28] ip_queue_xmit_[k]
  [29] __tcp_transmit_skb_[k]
  [30] tcp_write_xmit_[k]
  [31] __tcp_push_pending_frames_[k]
  [32] tcp_push_[k]
  [33] tcp_sendmsg_locked_[k]
  [34] tcp_sendmsg_[k]
  [35] inet6_sendmsg_[k]
  [36] sock_sendmsg_[k]
  [37] __sys_sendto_[k]
  [38] __x64_sys_sendto_[k]
  [39] do_syscall_64_[k]
  [40] entry_SYSCALL_64_after_hwframe_[k]
  [41] __libc_send
  [42] one.nio.net.NativeSocket.write
  [43] one.nio.net.Session$ArrayQueueItem.write
  [44] one.nio.net.Session.write
  [45] one.nio.net.Session.write
  [46] one.nio.http.HttpSession.writeResponse
  [47] one.nio.http.HttpSession.sendResponse
  [48] RequestHandler2_status.handleRequest
  [49] one.nio.http.HttpServer.handleRequest
  [50] one.nio.http.HttpSession.handleParsedRequest
  [51] one.nio.http.HttpSession.processHttpBuffer
  [52] one.nio.http.HttpSession.processRead
  [53] one.nio.net.Session.process
  [54] one.nio.server.SelectorThread.run

--- 30529762 ns (0.44%), 3 samples
  [ 0] tcp_current_mss_[k]
  [ 1] tcp_send_mss_[k]
  [ 2] tcp_sendmsg_locked_[k]
  [ 3] tcp_sendmsg_[k]
  [ 4] inet6_sendmsg_[k]
  [ 5] sock_sendmsg_[k]
  [ 6] __sys_sendto_[k]
  [ 7] __x64_sys_sendto_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __libc_send
  [11] one.nio.net.NativeSocket.write
  [12] one.nio.net.Session$ArrayQueueItem.write
  [13] one.nio.net.Session.write
  [14] one.nio.net.Session.write
  [15] one.nio.http.HttpSession.writeResponse
  [16] one.nio.http.HttpSession.sendResponse
  [17] RequestHandler2_status.handleRequest
  [18] one.nio.http.HttpServer.handleRequest
  [19] one.nio.http.HttpSession.handleParsedRequest
  [20] one.nio.http.HttpSession.processHttpBuffer
  [21] one.nio.http.HttpSession.processRead
  [22] one.nio.net.Session.process
  [23] one.nio.server.SelectorThread.run

--- 30523928 ns (0.44%), 3 samples
  [ 0] _raw_spin_unlock_bh_[k]
  [ 1] tcp_recvmsg_[k]
  [ 2] inet6_recvmsg_[k]
  [ 3] sock_recvmsg_[k]
  [ 4] __sys_recvfrom_[k]
  [ 5] __x64_sys_recvfrom_[k]
  [ 6] do_syscall_64_[k]
  [ 7] entry_SYSCALL_64_after_hwframe_[k]
  [ 8] __GI___recv
  [ 9] one.nio.net.NativeSocket.read
  [10] one.nio.net.Session.read
  [11] one.nio.http.HttpSession.processRead
  [12] one.nio.net.Session.process
  [13] one.nio.server.SelectorThread.run

--- 30516587 ns (0.44%), 3 samples
  [ 0] do_softirq.part.20_[k]
  [ 1] __local_bh_enable_ip_[k]
  [ 2] ip_finish_output2_[k]
  [ 3] __ip_finish_output_[k]
  [ 4] ip_finish_output_[k]
  [ 5] ip_output_[k]
  [ 6] ip_local_out_[k]
  [ 7] __ip_queue_xmit_[k]
  [ 8] ip_queue_xmit_[k]
  [ 9] __tcp_transmit_skb_[k]
  [10] tcp_write_xmit_[k]
  [11] __tcp_push_pending_frames_[k]
  [12] tcp_push_[k]
  [13] tcp_sendmsg_locked_[k]
  [14] tcp_sendmsg_[k]
  [15] inet6_sendmsg_[k]
  [16] sock_sendmsg_[k]
  [17] __sys_sendto_[k]
  [18] __x64_sys_sendto_[k]
  [19] do_syscall_64_[k]
  [20] entry_SYSCALL_64_after_hwframe_[k]
  [21] __libc_send
  [22] one.nio.net.NativeSocket.write
  [23] one.nio.net.Session$ArrayQueueItem.write
  [24] one.nio.net.Session.write
  [25] one.nio.net.Session.write
  [26] one.nio.http.HttpSession.writeResponse
  [27] one.nio.http.HttpSession.sendResponse
  [28] RequestHandler2_status.handleRequest
  [29] one.nio.http.HttpServer.handleRequest
  [30] one.nio.http.HttpSession.handleParsedRequest
  [31] one.nio.http.HttpSession.processHttpBuffer
  [32] one.nio.http.HttpSession.processRead
  [33] one.nio.net.Session.process
  [34] one.nio.server.SelectorThread.run

--- 30486687 ns (0.44%), 3 samples
  [ 0] aa_label_sk_perm.part.4_[k]
  [ 1] aa_sk_perm_[k]
  [ 2] aa_sock_msg_perm_[k]
  [ 3] apparmor_socket_sendmsg_[k]
  [ 4] security_socket_sendmsg_[k]
  [ 5] sock_sendmsg_[k]
  [ 6] __sys_sendto_[k]
  [ 7] __x64_sys_sendto_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __libc_send
  [11] one.nio.net.NativeSocket.write
  [12] one.nio.net.Session$ArrayQueueItem.write
  [13] one.nio.net.Session.write
  [14] one.nio.net.Session.write
  [15] one.nio.http.HttpSession.writeResponse
  [16] one.nio.http.HttpSession.sendResponse
  [17] RequestHandler2_status.handleRequest
  [18] one.nio.http.HttpServer.handleRequest
  [19] one.nio.http.HttpSession.handleParsedRequest
  [20] one.nio.http.HttpSession.processHttpBuffer
  [21] one.nio.http.HttpSession.processRead
  [22] one.nio.net.Session.process
  [23] one.nio.server.SelectorThread.run

--- 20435774 ns (0.30%), 2 samples
  [ 0] __sk_dst_check_[k]
  [ 1] ip_queue_xmit_[k]
  [ 2] __tcp_transmit_skb_[k]
  [ 3] tcp_write_xmit_[k]
  [ 4] __tcp_push_pending_frames_[k]
  [ 5] tcp_push_[k]
  [ 6] tcp_sendmsg_locked_[k]
  [ 7] tcp_sendmsg_[k]
  [ 8] inet6_sendmsg_[k]
  [ 9] sock_sendmsg_[k]
  [10] __sys_sendto_[k]
  [11] __x64_sys_sendto_[k]
  [12] do_syscall_64_[k]
  [13] entry_SYSCALL_64_after_hwframe_[k]
  [14] __libc_send
  [15] one.nio.net.NativeSocket.write
  [16] one.nio.net.Session$ArrayQueueItem.write
  [17] one.nio.net.Session.write
  [18] one.nio.net.Session.write
  [19] one.nio.http.HttpSession.writeResponse
  [20] one.nio.http.HttpSession.sendResponse
  [21] RequestHandler2_status.handleRequest
  [22] one.nio.http.HttpServer.handleRequest
  [23] one.nio.http.HttpSession.handleParsedRequest
  [24] one.nio.http.HttpSession.processHttpBuffer
  [25] one.nio.http.HttpSession.processRead
  [26] one.nio.net.Session.process
  [27] one.nio.server.SelectorThread.run

--- 20434610 ns (0.30%), 2 samples
  [ 0] ipv4_mtu_[k]
  [ 1] ip_finish_output_[k]
  [ 2] ip_output_[k]
  [ 3] ip_local_out_[k]
  [ 4] __ip_queue_xmit_[k]
  [ 5] ip_queue_xmit_[k]
  [ 6] __tcp_transmit_skb_[k]
  [ 7] tcp_write_xmit_[k]
  [ 8] __tcp_push_pending_frames_[k]
  [ 9] tcp_push_[k]
  [10] tcp_sendmsg_locked_[k]
  [11] tcp_sendmsg_[k]
  [12] inet6_sendmsg_[k]
  [13] sock_sendmsg_[k]
  [14] __sys_sendto_[k]
  [15] __x64_sys_sendto_[k]
  [16] do_syscall_64_[k]
  [17] entry_SYSCALL_64_after_hwframe_[k]
  [18] __libc_send
  [19] one.nio.net.NativeSocket.write
  [20] one.nio.net.Session$ArrayQueueItem.write
  [21] one.nio.net.Session.write
  [22] one.nio.net.Session.write
  [23] one.nio.http.HttpSession.writeResponse
  [24] one.nio.http.HttpSession.sendResponse
  [25] RequestHandler2_status.handleRequest
  [26] one.nio.http.HttpServer.handleRequest
  [27] one.nio.http.HttpSession.handleParsedRequest
  [28] one.nio.http.HttpSession.processHttpBuffer
  [29] one.nio.http.HttpSession.processRead
  [30] one.nio.net.Session.process
  [31] one.nio.server.SelectorThread.run

--- 20420069 ns (0.30%), 2 samples
  [ 0] __slab_alloc_[k]
  [ 1] __kmalloc_node_track_caller_[k]
  [ 2] __kmalloc_reserve.isra.62_[k]
  [ 3] __alloc_skb_[k]
  [ 4] sk_stream_alloc_skb_[k]
  [ 5] tcp_sendmsg_locked_[k]
  [ 6] tcp_sendmsg_[k]
  [ 7] inet6_sendmsg_[k]
  [ 8] sock_sendmsg_[k]
  [ 9] __sys_sendto_[k]
  [10] __x64_sys_sendto_[k]
  [11] do_syscall_64_[k]
  [12] entry_SYSCALL_64_after_hwframe_[k]
  [13] __libc_send
  [14] one.nio.net.NativeSocket.write
  [15] one.nio.net.Session$ArrayQueueItem.write
  [16] one.nio.net.Session.write
  [17] one.nio.net.Session.write
  [18] one.nio.http.HttpSession.writeResponse
  [19] one.nio.http.HttpSession.sendResponse
  [20] RequestHandler2_status.handleRequest
  [21] one.nio.http.HttpServer.handleRequest
  [22] one.nio.http.HttpSession.handleParsedRequest
  [23] one.nio.http.HttpSession.processHttpBuffer
  [24] one.nio.http.HttpSession.processRead
  [25] one.nio.net.Session.process
  [26] one.nio.server.SelectorThread.run

--- 20419844 ns (0.30%), 2 samples
  [ 0] __fget_light_[k]
  [ 1] __fdget_[k]
  [ 2] do_epoll_wait_[k]
  [ 3] __x64_sys_epoll_wait_[k]
  [ 4] do_syscall_64_[k]
  [ 5] entry_SYSCALL_64_after_hwframe_[k]
  [ 6] epoll_wait
  [ 7] [unknown]
  [ 8] one.nio.net.NativeSelector.epollWait
  [ 9] one.nio.net.NativeSelector.select
  [10] one.nio.server.SelectorThread.run

--- 20417544 ns (0.30%), 2 samples
  [ 0] check_bounds(int, int, int, Thread*)
  [ 1] jni_SetByteArrayRegion
  [ 2] Java_one_nio_net_NativeSocket_read
  [ 3] one.nio.net.NativeSocket.read
  [ 4] one.nio.net.Session.read
  [ 5] one.nio.http.HttpSession.processRead
  [ 6] one.nio.net.Session.process
  [ 7] one.nio.server.SelectorThread.run

--- 20415524 ns (0.30%), 2 samples
  [ 0] rb_insert_color_[k]
  [ 1] tcp_event_new_data_sent_[k]
  [ 2] tcp_write_xmit_[k]
  [ 3] __tcp_push_pending_frames_[k]
  [ 4] tcp_push_[k]
  [ 5] tcp_sendmsg_locked_[k]
  [ 6] tcp_sendmsg_[k]
  [ 7] inet6_sendmsg_[k]
  [ 8] sock_sendmsg_[k]
  [ 9] __sys_sendto_[k]
  [10] __x64_sys_sendto_[k]
  [11] do_syscall_64_[k]
  [12] entry_SYSCALL_64_after_hwframe_[k]
  [13] __libc_send
  [14] one.nio.net.NativeSocket.write
  [15] one.nio.net.Session$ArrayQueueItem.write
  [16] one.nio.net.Session.write
  [17] one.nio.net.Session.write
  [18] one.nio.http.HttpSession.writeResponse
  [19] one.nio.http.HttpSession.sendResponse
  [20] RequestHandler2_status.handleRequest
  [21] one.nio.http.HttpServer.handleRequest
  [22] one.nio.http.HttpSession.handleParsedRequest
  [23] one.nio.http.HttpSession.processHttpBuffer
  [24] one.nio.http.HttpSession.processRead
  [25] one.nio.net.Session.process
  [26] one.nio.server.SelectorThread.run

--- 20414958 ns (0.30%), 2 samples
  [ 0] skb_release_data_[k]
  [ 1] skb_release_all_[k]
  [ 2] __kfree_skb_[k]
  [ 3] tcp_recvmsg_[k]
  [ 4] inet6_recvmsg_[k]
  [ 5] sock_recvmsg_[k]
  [ 6] __sys_recvfrom_[k]
  [ 7] __x64_sys_recvfrom_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __GI___recv
  [11] one.nio.net.NativeSocket.read
  [12] one.nio.net.Session.read
  [13] one.nio.http.HttpSession.processRead
  [14] one.nio.net.Session.process
  [15] one.nio.server.SelectorThread.run

--- 20412614 ns (0.30%), 2 samples
  [ 0] sock_sendmsg_[k]
  [ 1] __sys_sendto_[k]
  [ 2] __x64_sys_sendto_[k]
  [ 3] do_syscall_64_[k]
  [ 4] entry_SYSCALL_64_after_hwframe_[k]
  [ 5] __libc_send
  [ 6] one.nio.net.NativeSocket.write
  [ 7] one.nio.net.Session$ArrayQueueItem.write
  [ 8] one.nio.net.Session.write
  [ 9] one.nio.net.Session.write
  [10] one.nio.http.HttpSession.writeResponse
  [11] one.nio.http.HttpSession.sendResponse
  [12] RequestHandler2_status.handleRequest
  [13] one.nio.http.HttpServer.handleRequest
  [14] one.nio.http.HttpSession.handleParsedRequest
  [15] one.nio.http.HttpSession.processHttpBuffer
  [16] one.nio.http.HttpSession.processRead
  [17] one.nio.net.Session.process
  [18] one.nio.server.SelectorThread.run

--- 20411900 ns (0.30%), 2 samples
  [ 0] Java_one_nio_net_NativeSelector_epollWait
  [ 1] one.nio.net.NativeSelector.epollWait
  [ 2] one.nio.net.NativeSelector.select
  [ 3] one.nio.server.SelectorThread.run

--- 20411770 ns (0.30%), 2 samples
  [ 0] kmem_cache_free_[k]
  [ 1] kfree_skbmem_[k]
  [ 2] __kfree_skb_[k]
  [ 3] tcp_clean_rtx_queue_[k]
  [ 4] tcp_ack_[k]
  [ 5] tcp_rcv_established_[k]
  [ 6] tcp_v4_do_rcv_[k]
  [ 7] tcp_v4_rcv_[k]
  [ 8] ip_protocol_deliver_rcu_[k]
  [ 9] ip_local_deliver_finish_[k]
  [10] ip_local_deliver_[k]
  [11] ip_rcv_finish_[k]
  [12] ip_rcv_[k]
  [13] __netif_receive_skb_one_core_[k]
  [14] __netif_receive_skb_[k]
  [15] process_backlog_[k]
  [16] net_rx_action_[k]
  [17] __softirqentry_text_start_[k]
  [18] do_softirq_own_stack_[k]
  [19] do_softirq.part.20_[k]
  [20] __local_bh_enable_ip_[k]
  [21] ip_finish_output2_[k]
  [22] __ip_finish_output_[k]
  [23] ip_finish_output_[k]
  [24] ip_output_[k]
  [25] ip_local_out_[k]
  [26] __ip_queue_xmit_[k]
  [27] ip_queue_xmit_[k]
  [28] __tcp_transmit_skb_[k]
  [29] tcp_write_xmit_[k]
  [30] __tcp_push_pending_frames_[k]
  [31] tcp_push_[k]
  [32] tcp_sendmsg_locked_[k]
  [33] tcp_sendmsg_[k]
  [34] inet6_sendmsg_[k]
  [35] sock_sendmsg_[k]
  [36] __sys_sendto_[k]
  [37] __x64_sys_sendto_[k]
  [38] do_syscall_64_[k]
  [39] entry_SYSCALL_64_after_hwframe_[k]
  [40] __libc_send
  [41] one.nio.net.NativeSocket.write
  [42] one.nio.net.Session$ArrayQueueItem.write
  [43] one.nio.net.Session.write
  [44] one.nio.net.Session.write
  [45] one.nio.http.HttpSession.writeResponse
  [46] one.nio.http.HttpSession.sendResponse
  [47] RequestHandler2_status.handleRequest
  [48] one.nio.http.HttpServer.handleRequest
  [49] one.nio.http.HttpSession.handleParsedRequest
  [50] one.nio.http.HttpSession.processHttpBuffer
  [51] one.nio.http.HttpSession.processRead
  [52] one.nio.net.Session.process
  [53] one.nio.server.SelectorThread.run

--- 20410332 ns (0.30%), 2 samples
  [ 0] aa_sk_perm_[k]
  [ 1] aa_sock_msg_perm_[k]
  [ 2] apparmor_socket_sendmsg_[k]
  [ 3] security_socket_sendmsg_[k]
  [ 4] sock_sendmsg_[k]
  [ 5] __sys_sendto_[k]
  [ 6] __x64_sys_sendto_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] __libc_send
  [10] one.nio.net.NativeSocket.write
  [11] one.nio.net.Session$ArrayQueueItem.write
  [12] one.nio.net.Session.write
  [13] one.nio.net.Session.write
  [14] one.nio.http.HttpSession.writeResponse
  [15] one.nio.http.HttpSession.sendResponse
  [16] RequestHandler2_status.handleRequest
  [17] one.nio.http.HttpServer.handleRequest
  [18] one.nio.http.HttpSession.handleParsedRequest
  [19] one.nio.http.HttpSession.processHttpBuffer
  [20] one.nio.http.HttpSession.processRead
  [21] one.nio.net.Session.process
  [22] one.nio.server.SelectorThread.run

--- 20410117 ns (0.30%), 2 samples
  [ 0] ipv4_conntrack_local?[nf_conntrack]_[k]
  [ 1] nf_hook_slow_[k]
  [ 2] __ip_local_out_[k]
  [ 3] ip_local_out_[k]
  [ 4] __ip_queue_xmit_[k]
  [ 5] ip_queue_xmit_[k]
  [ 6] __tcp_transmit_skb_[k]
  [ 7] tcp_write_xmit_[k]
  [ 8] __tcp_push_pending_frames_[k]
  [ 9] tcp_push_[k]
  [10] tcp_sendmsg_locked_[k]
  [11] tcp_sendmsg_[k]
  [12] inet6_sendmsg_[k]
  [13] sock_sendmsg_[k]
  [14] __sys_sendto_[k]
  [15] __x64_sys_sendto_[k]
  [16] do_syscall_64_[k]
  [17] entry_SYSCALL_64_after_hwframe_[k]
  [18] __libc_send
  [19] one.nio.net.NativeSocket.write
  [20] one.nio.net.Session$ArrayQueueItem.write
  [21] one.nio.net.Session.write
  [22] one.nio.net.Session.write
  [23] one.nio.http.HttpSession.writeResponse
  [24] one.nio.http.HttpSession.sendResponse
  [25] RequestHandler2_status.handleRequest
  [26] one.nio.http.HttpServer.handleRequest
  [27] one.nio.http.HttpSession.handleParsedRequest
  [28] one.nio.http.HttpSession.processHttpBuffer
  [29] one.nio.http.HttpSession.processRead
  [30] one.nio.net.Session.process
  [31] one.nio.server.SelectorThread.run

--- 20406151 ns (0.30%), 2 samples
  [ 0] ipv4_dst_check_[k]
  [ 1] tcp_v4_rcv_[k]
  [ 2] ip_protocol_deliver_rcu_[k]
  [ 3] ip_local_deliver_finish_[k]
  [ 4] ip_local_deliver_[k]
  [ 5] ip_rcv_finish_[k]
  [ 6] ip_rcv_[k]
  [ 7] __netif_receive_skb_one_core_[k]
  [ 8] __netif_receive_skb_[k]
  [ 9] process_backlog_[k]
  [10] net_rx_action_[k]
  [11] __softirqentry_text_start_[k]
  [12] do_softirq_own_stack_[k]
  [13] do_softirq.part.20_[k]
  [14] __local_bh_enable_ip_[k]
  [15] ip_finish_output2_[k]
  [16] __ip_finish_output_[k]
  [17] ip_finish_output_[k]
  [18] ip_output_[k]
  [19] ip_local_out_[k]
  [20] __ip_queue_xmit_[k]
  [21] ip_queue_xmit_[k]
  [22] __tcp_transmit_skb_[k]
  [23] tcp_write_xmit_[k]
  [24] __tcp_push_pending_frames_[k]
  [25] tcp_push_[k]
  [26] tcp_sendmsg_locked_[k]
  [27] tcp_sendmsg_[k]
  [28] inet6_sendmsg_[k]
  [29] sock_sendmsg_[k]
  [30] __sys_sendto_[k]
  [31] __x64_sys_sendto_[k]
  [32] do_syscall_64_[k]
  [33] entry_SYSCALL_64_after_hwframe_[k]
  [34] __libc_send
  [35] one.nio.net.NativeSocket.write
  [36] one.nio.net.Session$ArrayQueueItem.write
  [37] one.nio.net.Session.write
  [38] one.nio.net.Session.write
  [39] one.nio.http.HttpSession.writeResponse
  [40] one.nio.http.HttpSession.sendResponse
  [41] RequestHandler2_status.handleRequest
  [42] one.nio.http.HttpServer.handleRequest
  [43] one.nio.http.HttpSession.handleParsedRequest
  [44] one.nio.http.HttpSession.processHttpBuffer
  [45] one.nio.http.HttpSession.processRead
  [46] one.nio.net.Session.process
  [47] one.nio.server.SelectorThread.run

--- 20404719 ns (0.30%), 2 samples
  [ 0] tcp_event_data_recv_[k]
  [ 1] tcp_rcv_established_[k]
  [ 2] tcp_v4_do_rcv_[k]
  [ 3] tcp_v4_rcv_[k]
  [ 4] ip_protocol_deliver_rcu_[k]
  [ 5] ip_local_deliver_finish_[k]
  [ 6] ip_local_deliver_[k]
  [ 7] ip_rcv_finish_[k]
  [ 8] ip_rcv_[k]
  [ 9] __netif_receive_skb_one_core_[k]
  [10] __netif_receive_skb_[k]
  [11] process_backlog_[k]
  [12] net_rx_action_[k]
  [13] __softirqentry_text_start_[k]
  [14] do_softirq_own_stack_[k]
  [15] do_softirq.part.20_[k]
  [16] __local_bh_enable_ip_[k]
  [17] ip_finish_output2_[k]
  [18] __ip_finish_output_[k]
  [19] ip_finish_output_[k]
  [20] ip_output_[k]
  [21] ip_local_out_[k]
  [22] __ip_queue_xmit_[k]
  [23] ip_queue_xmit_[k]
  [24] __tcp_transmit_skb_[k]
  [25] tcp_write_xmit_[k]
  [26] __tcp_push_pending_frames_[k]
  [27] tcp_push_[k]
  [28] tcp_sendmsg_locked_[k]
  [29] tcp_sendmsg_[k]
  [30] inet6_sendmsg_[k]
  [31] sock_sendmsg_[k]
  [32] __sys_sendto_[k]
  [33] __x64_sys_sendto_[k]
  [34] do_syscall_64_[k]
  [35] entry_SYSCALL_64_after_hwframe_[k]
  [36] __libc_send
  [37] one.nio.net.NativeSocket.write
  [38] one.nio.net.Session$ArrayQueueItem.write
  [39] one.nio.net.Session.write
  [40] one.nio.net.Session.write
  [41] one.nio.http.HttpSession.writeResponse
  [42] one.nio.http.HttpSession.sendResponse
  [43] RequestHandler2_status.handleRequest
  [44] one.nio.http.HttpServer.handleRequest
  [45] one.nio.http.HttpSession.handleParsedRequest
  [46] one.nio.http.HttpSession.processHttpBuffer
  [47] one.nio.http.HttpSession.processRead
  [48] one.nio.net.Session.process
  [49] one.nio.server.SelectorThread.run

--- 20403812 ns (0.30%), 2 samples
  [ 0] kmem_cache_alloc_node_[k]
  [ 1] __alloc_skb_[k]
  [ 2] sk_stream_alloc_skb_[k]
  [ 3] tcp_sendmsg_locked_[k]
  [ 4] tcp_sendmsg_[k]
  [ 5] inet6_sendmsg_[k]
  [ 6] sock_sendmsg_[k]
  [ 7] __sys_sendto_[k]
  [ 8] __x64_sys_sendto_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __libc_send
  [12] one.nio.net.NativeSocket.write
  [13] one.nio.net.Session$ArrayQueueItem.write
  [14] one.nio.net.Session.write
  [15] one.nio.net.Session.write
  [16] one.nio.http.HttpSession.writeResponse
  [17] one.nio.http.HttpSession.sendResponse
  [18] RequestHandler2_status.handleRequest
  [19] one.nio.http.HttpServer.handleRequest
  [20] one.nio.http.HttpSession.handleParsedRequest
  [21] one.nio.http.HttpSession.processHttpBuffer
  [22] one.nio.http.HttpSession.processRead
  [23] one.nio.net.Session.process
  [24] one.nio.server.SelectorThread.run

--- 20401959 ns (0.30%), 2 samples
  [ 0] inet6_sendmsg_[k]
  [ 1] sock_sendmsg_[k]
  [ 2] __sys_sendto_[k]
  [ 3] __x64_sys_sendto_[k]
  [ 4] do_syscall_64_[k]
  [ 5] entry_SYSCALL_64_after_hwframe_[k]
  [ 6] __libc_send
  [ 7] one.nio.net.NativeSocket.write
  [ 8] one.nio.net.Session$ArrayQueueItem.write
  [ 9] one.nio.net.Session.write
  [10] one.nio.net.Session.write
  [11] one.nio.http.HttpSession.writeResponse
  [12] one.nio.http.HttpSession.sendResponse
  [13] RequestHandler2_status.handleRequest
  [14] one.nio.http.HttpServer.handleRequest
  [15] one.nio.http.HttpSession.handleParsedRequest
  [16] one.nio.http.HttpSession.processHttpBuffer
  [17] one.nio.http.HttpSession.processRead
  [18] one.nio.net.Session.process
  [19] one.nio.server.SelectorThread.run

--- 20400937 ns (0.30%), 2 samples
  [ 0] __skb_clone_[k]
  [ 1] skb_clone_[k]
  [ 2] __tcp_transmit_skb_[k]
  [ 3] tcp_write_xmit_[k]
  [ 4] __tcp_push_pending_frames_[k]
  [ 5] tcp_push_[k]
  [ 6] tcp_sendmsg_locked_[k]
  [ 7] tcp_sendmsg_[k]
  [ 8] inet6_sendmsg_[k]
  [ 9] sock_sendmsg_[k]
  [10] __sys_sendto_[k]
  [11] __x64_sys_sendto_[k]
  [12] do_syscall_64_[k]
  [13] entry_SYSCALL_64_after_hwframe_[k]
  [14] __libc_send
  [15] one.nio.net.NativeSocket.write
  [16] one.nio.net.Session$ArrayQueueItem.write
  [17] one.nio.net.Session.write
  [18] one.nio.net.Session.write
  [19] one.nio.http.HttpSession.writeResponse
  [20] one.nio.http.HttpSession.sendResponse
  [21] RequestHandler2_status.handleRequest
  [22] one.nio.http.HttpServer.handleRequest
  [23] one.nio.http.HttpSession.handleParsedRequest
  [24] one.nio.http.HttpSession.processHttpBuffer
  [25] one.nio.http.HttpSession.processRead
  [26] one.nio.net.Session.process
  [27] one.nio.server.SelectorThread.run

--- 20400201 ns (0.30%), 2 samples
  [ 0] tcp_send_delayed_ack_[k]
  [ 1] __tcp_ack_snd_check_[k]
  [ 2] tcp_rcv_established_[k]
  [ 3] tcp_v4_do_rcv_[k]
  [ 4] tcp_v4_rcv_[k]
  [ 5] ip_protocol_deliver_rcu_[k]
  [ 6] ip_local_deliver_finish_[k]
  [ 7] ip_local_deliver_[k]
  [ 8] ip_rcv_finish_[k]
  [ 9] ip_rcv_[k]
  [10] __netif_receive_skb_one_core_[k]
  [11] __netif_receive_skb_[k]
  [12] process_backlog_[k]
  [13] net_rx_action_[k]
  [14] __softirqentry_text_start_[k]
  [15] do_softirq_own_stack_[k]
  [16] do_softirq.part.20_[k]
  [17] __local_bh_enable_ip_[k]
  [18] ip_finish_output2_[k]
  [19] __ip_finish_output_[k]
  [20] ip_finish_output_[k]
  [21] ip_output_[k]
  [22] ip_local_out_[k]
  [23] __ip_queue_xmit_[k]
  [24] ip_queue_xmit_[k]
  [25] __tcp_transmit_skb_[k]
  [26] tcp_write_xmit_[k]
  [27] __tcp_push_pending_frames_[k]
  [28] tcp_push_[k]
  [29] tcp_sendmsg_locked_[k]
  [30] tcp_sendmsg_[k]
  [31] inet6_sendmsg_[k]
  [32] sock_sendmsg_[k]
  [33] __sys_sendto_[k]
  [34] __x64_sys_sendto_[k]
  [35] do_syscall_64_[k]
  [36] entry_SYSCALL_64_after_hwframe_[k]
  [37] __libc_send
  [38] one.nio.net.NativeSocket.write
  [39] one.nio.net.Session$ArrayQueueItem.write
  [40] one.nio.net.Session.write
  [41] one.nio.net.Session.write
  [42] one.nio.http.HttpSession.writeResponse
  [43] one.nio.http.HttpSession.sendResponse
  [44] RequestHandler2_status.handleRequest
  [45] one.nio.http.HttpServer.handleRequest
  [46] one.nio.http.HttpSession.handleParsedRequest
  [47] one.nio.http.HttpSession.processHttpBuffer
  [48] one.nio.http.HttpSession.processRead
  [49] one.nio.net.Session.process
  [50] one.nio.server.SelectorThread.run

--- 20399404 ns (0.30%), 2 samples
  [ 0] ipt_do_table?[ip_tables]_[k]
  [ 1] nf_hook_slow_[k]
  [ 2] __ip_local_out_[k]
  [ 3] ip_local_out_[k]
  [ 4] __ip_queue_xmit_[k]
  [ 5] ip_queue_xmit_[k]
  [ 6] __tcp_transmit_skb_[k]
  [ 7] tcp_write_xmit_[k]
  [ 8] __tcp_push_pending_frames_[k]
  [ 9] tcp_push_[k]
  [10] tcp_sendmsg_locked_[k]
  [11] tcp_sendmsg_[k]
  [12] inet6_sendmsg_[k]
  [13] sock_sendmsg_[k]
  [14] __sys_sendto_[k]
  [15] __x64_sys_sendto_[k]
  [16] do_syscall_64_[k]
  [17] entry_SYSCALL_64_after_hwframe_[k]
  [18] __libc_send
  [19] one.nio.net.NativeSocket.write
  [20] one.nio.net.Session$ArrayQueueItem.write
  [21] one.nio.net.Session.write
  [22] one.nio.net.Session.write
  [23] one.nio.http.HttpSession.writeResponse
  [24] one.nio.http.HttpSession.sendResponse
  [25] RequestHandler2_status.handleRequest
  [26] one.nio.http.HttpServer.handleRequest
  [27] one.nio.http.HttpSession.handleParsedRequest
  [28] one.nio.http.HttpSession.processHttpBuffer
  [29] one.nio.http.HttpSession.processRead
  [30] one.nio.net.Session.process
  [31] one.nio.server.SelectorThread.run

--- 20398194 ns (0.30%), 2 samples
  [ 0] validate_xmit_xfrm_[k]
  [ 1] __dev_queue_xmit_[k]
  [ 2] dev_queue_xmit_[k]
  [ 3] ip_finish_output2_[k]
  [ 4] __ip_finish_output_[k]
  [ 5] ip_finish_output_[k]
  [ 6] ip_output_[k]
  [ 7] ip_local_out_[k]
  [ 8] __ip_queue_xmit_[k]
  [ 9] ip_queue_xmit_[k]
  [10] __tcp_transmit_skb_[k]
  [11] tcp_write_xmit_[k]
  [12] __tcp_push_pending_frames_[k]
  [13] tcp_push_[k]
  [14] tcp_sendmsg_locked_[k]
  [15] tcp_sendmsg_[k]
  [16] inet6_sendmsg_[k]
  [17] sock_sendmsg_[k]
  [18] __sys_sendto_[k]
  [19] __x64_sys_sendto_[k]
  [20] do_syscall_64_[k]
  [21] entry_SYSCALL_64_after_hwframe_[k]
  [22] __libc_send
  [23] one.nio.net.NativeSocket.write
  [24] one.nio.net.Session$ArrayQueueItem.write
  [25] one.nio.net.Session.write
  [26] one.nio.net.Session.write
  [27] one.nio.http.HttpSession.writeResponse
  [28] one.nio.http.HttpSession.sendResponse
  [29] RequestHandler2_status.handleRequest
  [30] one.nio.http.HttpServer.handleRequest
  [31] one.nio.http.HttpSession.handleParsedRequest
  [32] one.nio.http.HttpSession.processHttpBuffer
  [33] one.nio.http.HttpSession.processRead
  [34] one.nio.net.Session.process
  [35] one.nio.server.SelectorThread.run

--- 20397834 ns (0.30%), 2 samples
  [ 0] sock_recvmsg_[k]
  [ 1] __sys_recvfrom_[k]
  [ 2] __x64_sys_recvfrom_[k]
  [ 3] do_syscall_64_[k]
  [ 4] entry_SYSCALL_64_after_hwframe_[k]
  [ 5] __GI___recv
  [ 6] one.nio.net.NativeSocket.read
  [ 7] one.nio.net.Session.read
  [ 8] one.nio.http.HttpSession.processRead
  [ 9] one.nio.net.Session.process
  [10] one.nio.server.SelectorThread.run

--- 20395220 ns (0.30%), 2 samples
  [ 0] one.nio.net.NativeSocket.write
  [ 1] one.nio.net.Session$ArrayQueueItem.write
  [ 2] one.nio.net.Session.write
  [ 3] one.nio.net.Session.write
  [ 4] one.nio.http.HttpSession.writeResponse
  [ 5] one.nio.http.HttpSession.sendResponse
  [ 6] RequestHandler2_status.handleRequest
  [ 7] one.nio.http.HttpServer.handleRequest
  [ 8] one.nio.http.HttpSession.handleParsedRequest
  [ 9] one.nio.http.HttpSession.processHttpBuffer
  [10] one.nio.http.HttpSession.processRead
  [11] one.nio.net.Session.process
  [12] one.nio.server.SelectorThread.run

--- 20393738 ns (0.30%), 2 samples
  [ 0] eth_type_trans_[k]
  [ 1] dev_hard_start_xmit_[k]
  [ 2] __dev_queue_xmit_[k]
  [ 3] dev_queue_xmit_[k]
  [ 4] ip_finish_output2_[k]
  [ 5] __ip_finish_output_[k]
  [ 6] ip_finish_output_[k]
  [ 7] ip_output_[k]
  [ 8] ip_local_out_[k]
  [ 9] __ip_queue_xmit_[k]
  [10] ip_queue_xmit_[k]
  [11] __tcp_transmit_skb_[k]
  [12] tcp_write_xmit_[k]
  [13] __tcp_push_pending_frames_[k]
  [14] tcp_push_[k]
  [15] tcp_sendmsg_locked_[k]
  [16] tcp_sendmsg_[k]
  [17] inet6_sendmsg_[k]
  [18] sock_sendmsg_[k]
  [19] __sys_sendto_[k]
  [20] __x64_sys_sendto_[k]
  [21] do_syscall_64_[k]
  [22] entry_SYSCALL_64_after_hwframe_[k]
  [23] __libc_send
  [24] one.nio.net.NativeSocket.write
  [25] one.nio.net.Session$ArrayQueueItem.write
  [26] one.nio.net.Session.write
  [27] one.nio.net.Session.write
  [28] one.nio.http.HttpSession.writeResponse
  [29] one.nio.http.HttpSession.sendResponse
  [30] RequestHandler2_status.handleRequest
  [31] one.nio.http.HttpServer.handleRequest
  [32] one.nio.http.HttpSession.handleParsedRequest
  [33] one.nio.http.HttpSession.processHttpBuffer
  [34] one.nio.http.HttpSession.processRead
  [35] one.nio.net.Session.process
  [36] one.nio.server.SelectorThread.run

--- 20387809 ns (0.30%), 2 samples
  [ 0] validate_xmit_skb_[k]
  [ 1] __dev_queue_xmit_[k]
  [ 2] dev_queue_xmit_[k]
  [ 3] ip_finish_output2_[k]
  [ 4] __ip_finish_output_[k]
  [ 5] ip_finish_output_[k]
  [ 6] ip_output_[k]
  [ 7] ip_local_out_[k]
  [ 8] __ip_queue_xmit_[k]
  [ 9] ip_queue_xmit_[k]
  [10] __tcp_transmit_skb_[k]
  [11] tcp_write_xmit_[k]
  [12] __tcp_push_pending_frames_[k]
  [13] tcp_push_[k]
  [14] tcp_sendmsg_locked_[k]
  [15] tcp_sendmsg_[k]
  [16] inet6_sendmsg_[k]
  [17] sock_sendmsg_[k]
  [18] __sys_sendto_[k]
  [19] __x64_sys_sendto_[k]
  [20] do_syscall_64_[k]
  [21] entry_SYSCALL_64_after_hwframe_[k]
  [22] __libc_send
  [23] one.nio.net.NativeSocket.write
  [24] one.nio.net.Session$ArrayQueueItem.write
  [25] one.nio.net.Session.write
  [26] one.nio.net.Session.write
  [27] one.nio.http.HttpSession.writeResponse
  [28] one.nio.http.HttpSession.sendResponse
  [29] RequestHandler2_status.handleRequest
  [30] one.nio.http.HttpServer.handleRequest
  [31] one.nio.http.HttpSession.handleParsedRequest
  [32] one.nio.http.HttpSession.processHttpBuffer
  [33] one.nio.http.HttpSession.processRead
  [34] one.nio.net.Session.process
  [35] one.nio.server.SelectorThread.run

--- 20387658 ns (0.30%), 2 samples
  [ 0] jbyte_disjoint_arraycopy
  [ 1] java.lang.StringUTF16.checkIndex
  [ 2] java.lang.StringUTF16.charAt
  [ 3] java.lang.String.charAt
  [ 4] one.nio.util.Utf8.length
  [ 5] one.nio.util.ByteArrayBuilder.append
  [ 6] one.nio.http.Response.toBytes
  [ 7] one.nio.http.HttpSession.writeResponse
  [ 8] one.nio.http.HttpSession.sendResponse
  [ 9] RequestHandler2_status.handleRequest
  [10] one.nio.http.HttpServer.handleRequest
  [11] one.nio.http.HttpSession.handleParsedRequest
  [12] one.nio.http.HttpSession.processHttpBuffer
  [13] one.nio.http.HttpSession.processRead
  [14] one.nio.net.Session.process
  [15] one.nio.server.SelectorThread.run

--- 20386754 ns (0.30%), 2 samples
  [ 0] __audit_syscall_exit_[k]
  [ 1] syscall_slow_exit_work_[k]
  [ 2] do_syscall_64_[k]
  [ 3] entry_SYSCALL_64_after_hwframe_[k]
  [ 4] __libc_send
  [ 5] one.nio.net.NativeSocket.write
  [ 6] one.nio.net.Session$ArrayQueueItem.write
  [ 7] one.nio.net.Session.write
  [ 8] one.nio.net.Session.write
  [ 9] one.nio.http.HttpSession.writeResponse
  [10] one.nio.http.HttpSession.sendResponse
  [11] RequestHandler2_status.handleRequest
  [12] one.nio.http.HttpServer.handleRequest
  [13] one.nio.http.HttpSession.handleParsedRequest
  [14] one.nio.http.HttpSession.processHttpBuffer
  [15] one.nio.http.HttpSession.processRead
  [16] one.nio.net.Session.process
  [17] one.nio.server.SelectorThread.run

--- 20386125 ns (0.30%), 2 samples
  [ 0] ipv4_dst_check_[k]
  [ 1] __ip_queue_xmit_[k]
  [ 2] ip_queue_xmit_[k]
  [ 3] __tcp_transmit_skb_[k]
  [ 4] tcp_write_xmit_[k]
  [ 5] __tcp_push_pending_frames_[k]
  [ 6] tcp_push_[k]
  [ 7] tcp_sendmsg_locked_[k]
  [ 8] tcp_sendmsg_[k]
  [ 9] inet6_sendmsg_[k]
  [10] sock_sendmsg_[k]
  [11] __sys_sendto_[k]
  [12] __x64_sys_sendto_[k]
  [13] do_syscall_64_[k]
  [14] entry_SYSCALL_64_after_hwframe_[k]
  [15] __libc_send
  [16] one.nio.net.NativeSocket.write
  [17] one.nio.net.Session$ArrayQueueItem.write
  [18] one.nio.net.Session.write
  [19] one.nio.net.Session.write
  [20] one.nio.http.HttpSession.writeResponse
  [21] one.nio.http.HttpSession.sendResponse
  [22] RequestHandler2_status.handleRequest
  [23] one.nio.http.HttpServer.handleRequest
  [24] one.nio.http.HttpSession.handleParsedRequest
  [25] one.nio.http.HttpSession.processHttpBuffer
  [26] one.nio.http.HttpSession.processRead
  [27] one.nio.net.Session.process
  [28] one.nio.server.SelectorThread.run

--- 20385933 ns (0.30%), 2 samples
  [ 0] __cgroup_bpf_run_filter_skb_[k]
  [ 1] ip_finish_output_[k]
  [ 2] ip_output_[k]
  [ 3] ip_local_out_[k]
  [ 4] __ip_queue_xmit_[k]
  [ 5] ip_queue_xmit_[k]
  [ 6] __tcp_transmit_skb_[k]
  [ 7] tcp_write_xmit_[k]
  [ 8] __tcp_push_pending_frames_[k]
  [ 9] tcp_push_[k]
  [10] tcp_sendmsg_locked_[k]
  [11] tcp_sendmsg_[k]
  [12] inet6_sendmsg_[k]
  [13] sock_sendmsg_[k]
  [14] __sys_sendto_[k]
  [15] __x64_sys_sendto_[k]
  [16] do_syscall_64_[k]
  [17] entry_SYSCALL_64_after_hwframe_[k]
  [18] __libc_send
  [19] one.nio.net.NativeSocket.write
  [20] one.nio.net.Session$ArrayQueueItem.write
  [21] one.nio.net.Session.write
  [22] one.nio.net.Session.write
  [23] one.nio.http.HttpSession.writeResponse
  [24] one.nio.http.HttpSession.sendResponse
  [25] RequestHandler2_status.handleRequest
  [26] one.nio.http.HttpServer.handleRequest
  [27] one.nio.http.HttpSession.handleParsedRequest
  [28] one.nio.http.HttpSession.processHttpBuffer
  [29] one.nio.http.HttpSession.processRead
  [30] one.nio.net.Session.process
  [31] one.nio.server.SelectorThread.run

--- 20385508 ns (0.30%), 2 samples
  [ 0] __check_object_size_[k]
  [ 1] tcp_sendmsg_locked_[k]
  [ 2] tcp_sendmsg_[k]
  [ 3] inet6_sendmsg_[k]
  [ 4] sock_sendmsg_[k]
  [ 5] __sys_sendto_[k]
  [ 6] __x64_sys_sendto_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] __libc_send
  [10] one.nio.net.NativeSocket.write
  [11] one.nio.net.Session$ArrayQueueItem.write
  [12] one.nio.net.Session.write
  [13] one.nio.net.Session.write
  [14] one.nio.http.HttpSession.writeResponse
  [15] one.nio.http.HttpSession.sendResponse
  [16] RequestHandler2_status.handleRequest
  [17] one.nio.http.HttpServer.handleRequest
  [18] one.nio.http.HttpSession.handleParsedRequest
  [19] one.nio.http.HttpSession.processHttpBuffer
  [20] one.nio.http.HttpSession.processRead
  [21] one.nio.net.Session.process
  [22] one.nio.server.SelectorThread.run

--- 20382959 ns (0.30%), 2 samples
  [ 0] clock_gettime
  [ 1] [unknown]
  [ 2] [unknown]
  [ 3] one.nio.net.NativeSelector.epollWait
  [ 4] one.nio.net.NativeSelector.select
  [ 5] one.nio.server.SelectorThread.run

--- 20382377 ns (0.30%), 2 samples
  [ 0] aa_profile_af_perm_[k]
  [ 1] aa_label_sk_perm.part.4_[k]
  [ 2] aa_sk_perm_[k]
  [ 3] aa_sock_msg_perm_[k]
  [ 4] apparmor_socket_recvmsg_[k]
  [ 5] security_socket_recvmsg_[k]
  [ 6] sock_recvmsg_[k]
  [ 7] __sys_recvfrom_[k]
  [ 8] __x64_sys_recvfrom_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __GI___recv
  [12] one.nio.net.NativeSocket.read
  [13] one.nio.net.Session.read
  [14] one.nio.http.HttpSession.processRead
  [15] one.nio.net.Session.process
  [16] one.nio.server.SelectorThread.run

--- 20379723 ns (0.30%), 2 samples
  [ 0] memset_erms_[k]
  [ 1] __kmalloc_reserve.isra.62_[k]
  [ 2] __alloc_skb_[k]
  [ 3] sk_stream_alloc_skb_[k]
  [ 4] tcp_sendmsg_locked_[k]
  [ 5] tcp_sendmsg_[k]
  [ 6] inet6_sendmsg_[k]
  [ 7] sock_sendmsg_[k]
  [ 8] __sys_sendto_[k]
  [ 9] __x64_sys_sendto_[k]
  [10] do_syscall_64_[k]
  [11] entry_SYSCALL_64_after_hwframe_[k]
  [12] __libc_send
  [13] one.nio.net.NativeSocket.write
  [14] one.nio.net.Session$ArrayQueueItem.write
  [15] one.nio.net.Session.write
  [16] one.nio.net.Session.write
  [17] one.nio.http.HttpSession.writeResponse
  [18] one.nio.http.HttpSession.sendResponse
  [19] RequestHandler2_status.handleRequest
  [20] one.nio.http.HttpServer.handleRequest
  [21] one.nio.http.HttpSession.handleParsedRequest
  [22] one.nio.http.HttpSession.processHttpBuffer
  [23] one.nio.http.HttpSession.processRead
  [24] one.nio.net.Session.process
  [25] one.nio.server.SelectorThread.run

--- 20379600 ns (0.30%), 2 samples
  [ 0] __softirqentry_text_start_[k]
  [ 1] do_softirq_own_stack_[k]
  [ 2] do_softirq.part.20_[k]
  [ 3] __local_bh_enable_ip_[k]
  [ 4] ip_finish_output2_[k]
  [ 5] __ip_finish_output_[k]
  [ 6] ip_finish_output_[k]
  [ 7] ip_output_[k]
  [ 8] ip_local_out_[k]
  [ 9] __ip_queue_xmit_[k]
  [10] ip_queue_xmit_[k]
  [11] __tcp_transmit_skb_[k]
  [12] tcp_write_xmit_[k]
  [13] __tcp_push_pending_frames_[k]
  [14] tcp_push_[k]
  [15] tcp_sendmsg_locked_[k]
  [16] tcp_sendmsg_[k]
  [17] inet6_sendmsg_[k]
  [18] sock_sendmsg_[k]
  [19] __sys_sendto_[k]
  [20] __x64_sys_sendto_[k]
  [21] do_syscall_64_[k]
  [22] entry_SYSCALL_64_after_hwframe_[k]
  [23] __libc_send
  [24] one.nio.net.NativeSocket.write
  [25] one.nio.net.Session$ArrayQueueItem.write
  [26] one.nio.net.Session.write
  [27] one.nio.net.Session.write
  [28] one.nio.http.HttpSession.writeResponse
  [29] one.nio.http.HttpSession.sendResponse
  [30] RequestHandler2_status.handleRequest
  [31] one.nio.http.HttpServer.handleRequest
  [32] one.nio.http.HttpSession.handleParsedRequest
  [33] one.nio.http.HttpSession.processHttpBuffer
  [34] one.nio.http.HttpSession.processRead
  [35] one.nio.net.Session.process
  [36] one.nio.server.SelectorThread.run

--- 20374468 ns (0.30%), 2 samples
  [ 0] one.nio.net.Session.write
  [ 1] one.nio.net.Session.write
  [ 2] one.nio.http.HttpSession.writeResponse
  [ 3] one.nio.http.HttpSession.sendResponse
  [ 4] RequestHandler2_status.handleRequest
  [ 5] one.nio.http.HttpServer.handleRequest
  [ 6] one.nio.http.HttpSession.handleParsedRequest
  [ 7] one.nio.http.HttpSession.processHttpBuffer
  [ 8] one.nio.http.HttpSession.processRead
  [ 9] one.nio.net.Session.process
  [10] one.nio.server.SelectorThread.run

--- 20374322 ns (0.30%), 2 samples
  [ 0] HandleMark::pop_and_restore()
  [ 1] Java_one_nio_net_NativeSocket_read
  [ 2] one.nio.net.NativeSocket.read
  [ 3] one.nio.net.Session.read
  [ 4] one.nio.http.HttpSession.processRead
  [ 5] one.nio.net.Session.process
  [ 6] one.nio.server.SelectorThread.run

--- 20373470 ns (0.30%), 2 samples
  [ 0] netif_skb_features_[k]
  [ 1] validate_xmit_skb_[k]
  [ 2] __dev_queue_xmit_[k]
  [ 3] dev_queue_xmit_[k]
  [ 4] ip_finish_output2_[k]
  [ 5] __ip_finish_output_[k]
  [ 6] ip_finish_output_[k]
  [ 7] ip_output_[k]
  [ 8] ip_local_out_[k]
  [ 9] __ip_queue_xmit_[k]
  [10] ip_queue_xmit_[k]
  [11] __tcp_transmit_skb_[k]
  [12] tcp_write_xmit_[k]
  [13] __tcp_push_pending_frames_[k]
  [14] tcp_push_[k]
  [15] tcp_sendmsg_locked_[k]
  [16] tcp_sendmsg_[k]
  [17] inet6_sendmsg_[k]
  [18] sock_sendmsg_[k]
  [19] __sys_sendto_[k]
  [20] __x64_sys_sendto_[k]
  [21] do_syscall_64_[k]
  [22] entry_SYSCALL_64_after_hwframe_[k]
  [23] __libc_send
  [24] one.nio.net.NativeSocket.write
  [25] one.nio.net.Session$ArrayQueueItem.write
  [26] one.nio.net.Session.write
  [27] one.nio.net.Session.write
  [28] one.nio.http.HttpSession.writeResponse
  [29] one.nio.http.HttpSession.sendResponse
  [30] RequestHandler2_status.handleRequest
  [31] one.nio.http.HttpServer.handleRequest
  [32] one.nio.http.HttpSession.handleParsedRequest
  [33] one.nio.http.HttpSession.processHttpBuffer
  [34] one.nio.http.HttpSession.processRead
  [35] one.nio.net.Session.process
  [36] one.nio.server.SelectorThread.run

--- 20371726 ns (0.30%), 2 samples
  [ 0] process_backlog_[k]
  [ 1] net_rx_action_[k]
  [ 2] __softirqentry_text_start_[k]
  [ 3] do_softirq_own_stack_[k]
  [ 4] do_softirq.part.20_[k]
  [ 5] __local_bh_enable_ip_[k]
  [ 6] ip_finish_output2_[k]
  [ 7] __ip_finish_output_[k]
  [ 8] ip_finish_output_[k]
  [ 9] ip_output_[k]
  [10] ip_local_out_[k]
  [11] __ip_queue_xmit_[k]
  [12] ip_queue_xmit_[k]
  [13] __tcp_transmit_skb_[k]
  [14] tcp_write_xmit_[k]
  [15] __tcp_push_pending_frames_[k]
  [16] tcp_push_[k]
  [17] tcp_sendmsg_locked_[k]
  [18] tcp_sendmsg_[k]
  [19] inet6_sendmsg_[k]
  [20] sock_sendmsg_[k]
  [21] __sys_sendto_[k]
  [22] __x64_sys_sendto_[k]
  [23] do_syscall_64_[k]
  [24] entry_SYSCALL_64_after_hwframe_[k]
  [25] __libc_send
  [26] one.nio.net.NativeSocket.write
  [27] one.nio.net.Session$ArrayQueueItem.write
  [28] one.nio.net.Session.write
  [29] one.nio.net.Session.write
  [30] one.nio.http.HttpSession.writeResponse
  [31] one.nio.http.HttpSession.sendResponse
  [32] RequestHandler2_status.handleRequest
  [33] one.nio.http.HttpServer.handleRequest
  [34] one.nio.http.HttpSession.handleParsedRequest
  [35] one.nio.http.HttpSession.processHttpBuffer
  [36] one.nio.http.HttpSession.processRead
  [37] one.nio.net.Session.process
  [38] one.nio.server.SelectorThread.run

--- 20370646 ns (0.30%), 2 samples
  [ 0] [vdso]
  [ 1] clock_gettime
  [ 2] [unknown]
  [ 3] [unknown]
  [ 4] one.nio.net.NativeSelector.epollWait
  [ 5] one.nio.net.NativeSelector.select
  [ 6] one.nio.server.SelectorThread.run

--- 20370557 ns (0.30%), 2 samples
  [ 0] one.nio.net.NativeSocket.read
  [ 1] one.nio.http.HttpSession.processRead
  [ 2] one.nio.net.Session.process
  [ 3] one.nio.server.SelectorThread.run

--- 20368912 ns (0.30%), 2 samples
  [ 0] sock_poll_[k]
  [ 1] ep_item_poll.isra.16_[k]
  [ 2] ep_send_events_proc_[k]
  [ 3] ep_scan_ready_list.constprop.20_[k]
  [ 4] ep_poll_[k]
  [ 5] do_epoll_wait_[k]
  [ 6] __x64_sys_epoll_wait_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] epoll_wait
  [10] [unknown]
  [11] one.nio.net.NativeSelector.epollWait
  [12] one.nio.net.NativeSelector.select
  [13] one.nio.server.SelectorThread.run

--- 20368175 ns (0.30%), 2 samples
  [ 0] Java_one_nio_net_NativeSocket_read
  [ 1] one.nio.net.NativeSocket.read
  [ 2] one.nio.net.Session.read
  [ 3] one.nio.http.HttpSession.processRead
  [ 4] one.nio.net.Session.process
  [ 5] one.nio.server.SelectorThread.run

--- 20365332 ns (0.30%), 2 samples
  [ 0] clock_gettime
  [ 1] one.nio.net.NativeSelector.epollWait
  [ 2] one.nio.net.NativeSelector.select
  [ 3] one.nio.server.SelectorThread.run

--- 20364816 ns (0.30%), 2 samples
  [ 0] tcp_write_xmit_[k]
  [ 1] __tcp_push_pending_frames_[k]
  [ 2] tcp_push_[k]
  [ 3] tcp_sendmsg_locked_[k]
  [ 4] tcp_sendmsg_[k]
  [ 5] inet6_sendmsg_[k]
  [ 6] sock_sendmsg_[k]
  [ 7] __sys_sendto_[k]
  [ 8] __x64_sys_sendto_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __libc_send
  [12] one.nio.net.NativeSocket.write
  [13] one.nio.net.Session$ArrayQueueItem.write
  [14] one.nio.net.Session.write
  [15] one.nio.net.Session.write
  [16] one.nio.http.HttpSession.writeResponse
  [17] one.nio.http.HttpSession.sendResponse
  [18] RequestHandler2_status.handleRequest
  [19] one.nio.http.HttpServer.handleRequest
  [20] one.nio.http.HttpSession.handleParsedRequest
  [21] one.nio.http.HttpSession.processHttpBuffer
  [22] one.nio.http.HttpSession.processRead
  [23] one.nio.net.Session.process
  [24] one.nio.server.SelectorThread.run

--- 20364713 ns (0.30%), 2 samples
  [ 0] kfree_[k]
  [ 1] skb_free_head_[k]
  [ 2] skb_release_data_[k]
  [ 3] skb_release_all_[k]
  [ 4] __kfree_skb_[k]
  [ 5] tcp_clean_rtx_queue_[k]
  [ 6] tcp_ack_[k]
  [ 7] tcp_rcv_established_[k]
  [ 8] tcp_v4_do_rcv_[k]
  [ 9] tcp_v4_rcv_[k]
  [10] ip_protocol_deliver_rcu_[k]
  [11] ip_local_deliver_finish_[k]
  [12] ip_local_deliver_[k]
  [13] ip_rcv_finish_[k]
  [14] ip_rcv_[k]
  [15] __netif_receive_skb_one_core_[k]
  [16] __netif_receive_skb_[k]
  [17] process_backlog_[k]
  [18] net_rx_action_[k]
  [19] __softirqentry_text_start_[k]
  [20] do_softirq_own_stack_[k]
  [21] do_softirq.part.20_[k]
  [22] __local_bh_enable_ip_[k]
  [23] ip_finish_output2_[k]
  [24] __ip_finish_output_[k]
  [25] ip_finish_output_[k]
  [26] ip_output_[k]
  [27] ip_local_out_[k]
  [28] __ip_queue_xmit_[k]
  [29] ip_queue_xmit_[k]
  [30] __tcp_transmit_skb_[k]
  [31] tcp_write_xmit_[k]
  [32] __tcp_push_pending_frames_[k]
  [33] tcp_push_[k]
  [34] tcp_sendmsg_locked_[k]
  [35] tcp_sendmsg_[k]
  [36] inet6_sendmsg_[k]
  [37] sock_sendmsg_[k]
  [38] __sys_sendto_[k]
  [39] __x64_sys_sendto_[k]
  [40] do_syscall_64_[k]
  [41] entry_SYSCALL_64_after_hwframe_[k]
  [42] __libc_send
  [43] one.nio.net.NativeSocket.write
  [44] one.nio.net.Session$ArrayQueueItem.write
  [45] one.nio.net.Session.write
  [46] one.nio.net.Session.write
  [47] one.nio.http.HttpSession.writeResponse
  [48] one.nio.http.HttpSession.sendResponse
  [49] RequestHandler2_status.handleRequest
  [50] one.nio.http.HttpServer.handleRequest
  [51] one.nio.http.HttpSession.handleParsedRequest
  [52] one.nio.http.HttpSession.processHttpBuffer
  [53] one.nio.http.HttpSession.processRead
  [54] one.nio.net.Session.process
  [55] one.nio.server.SelectorThread.run

--- 20364507 ns (0.30%), 2 samples
  [ 0] one.nio.http.HttpServer.handleRequest
  [ 1] one.nio.http.HttpSession.handleParsedRequest
  [ 2] one.nio.http.HttpSession.processHttpBuffer
  [ 3] one.nio.http.HttpSession.processRead
  [ 4] one.nio.net.Session.process
  [ 5] one.nio.server.SelectorThread.run

--- 20364449 ns (0.30%), 2 samples
  [ 0] nf_hook_slow_[k]
  [ 1] ip_local_out_[k]
  [ 2] __ip_queue_xmit_[k]
  [ 3] ip_queue_xmit_[k]
  [ 4] __tcp_transmit_skb_[k]
  [ 5] tcp_write_xmit_[k]
  [ 6] __tcp_push_pending_frames_[k]
  [ 7] tcp_push_[k]
  [ 8] tcp_sendmsg_locked_[k]
  [ 9] tcp_sendmsg_[k]
  [10] inet6_sendmsg_[k]
  [11] sock_sendmsg_[k]
  [12] __sys_sendto_[k]
  [13] __x64_sys_sendto_[k]
  [14] do_syscall_64_[k]
  [15] entry_SYSCALL_64_after_hwframe_[k]
  [16] __libc_send
  [17] one.nio.net.NativeSocket.write
  [18] one.nio.net.Session$ArrayQueueItem.write
  [19] one.nio.net.Session.write
  [20] one.nio.net.Session.write
  [21] one.nio.http.HttpSession.writeResponse
  [22] one.nio.http.HttpSession.sendResponse
  [23] RequestHandler2_status.handleRequest
  [24] one.nio.http.HttpServer.handleRequest
  [25] one.nio.http.HttpSession.handleParsedRequest
  [26] one.nio.http.HttpSession.processHttpBuffer
  [27] one.nio.http.HttpSession.processRead
  [28] one.nio.net.Session.process
  [29] one.nio.server.SelectorThread.run

--- 20363787 ns (0.30%), 2 samples
  [ 0] __fget_[k]
  [ 1] __fget_light_[k]
  [ 2] __fdget_[k]
  [ 3] do_epoll_wait_[k]
  [ 4] __x64_sys_epoll_wait_[k]
  [ 5] do_syscall_64_[k]
  [ 6] entry_SYSCALL_64_after_hwframe_[k]
  [ 7] epoll_wait
  [ 8] [unknown]
  [ 9] one.nio.net.NativeSelector.epollWait
  [10] one.nio.net.NativeSelector.select
  [11] one.nio.server.SelectorThread.run

--- 20362941 ns (0.30%), 2 samples
  [ 0] security_sock_rcv_skb_[k]
  [ 1] sk_filter_trim_cap_[k]
  [ 2] tcp_v4_rcv_[k]
  [ 3] ip_protocol_deliver_rcu_[k]
  [ 4] ip_local_deliver_finish_[k]
  [ 5] ip_local_deliver_[k]
  [ 6] ip_rcv_finish_[k]
  [ 7] ip_rcv_[k]
  [ 8] __netif_receive_skb_one_core_[k]
  [ 9] __netif_receive_skb_[k]
  [10] process_backlog_[k]
  [11] net_rx_action_[k]
  [12] __softirqentry_text_start_[k]
  [13] do_softirq_own_stack_[k]
  [14] do_softirq.part.20_[k]
  [15] __local_bh_enable_ip_[k]
  [16] ip_finish_output2_[k]
  [17] __ip_finish_output_[k]
  [18] ip_finish_output_[k]
  [19] ip_output_[k]
  [20] ip_local_out_[k]
  [21] __ip_queue_xmit_[k]
  [22] ip_queue_xmit_[k]
  [23] __tcp_transmit_skb_[k]
  [24] tcp_write_xmit_[k]
  [25] __tcp_push_pending_frames_[k]
  [26] tcp_push_[k]
  [27] tcp_sendmsg_locked_[k]
  [28] tcp_sendmsg_[k]
  [29] inet6_sendmsg_[k]
  [30] sock_sendmsg_[k]
  [31] __sys_sendto_[k]
  [32] __x64_sys_sendto_[k]
  [33] do_syscall_64_[k]
  [34] entry_SYSCALL_64_after_hwframe_[k]
  [35] __libc_send
  [36] one.nio.net.NativeSocket.write
  [37] one.nio.net.Session$ArrayQueueItem.write
  [38] one.nio.net.Session.write
  [39] one.nio.net.Session.write
  [40] one.nio.http.HttpSession.writeResponse
  [41] one.nio.http.HttpSession.sendResponse
  [42] RequestHandler2_status.handleRequest
  [43] one.nio.http.HttpServer.handleRequest
  [44] one.nio.http.HttpSession.handleParsedRequest
  [45] one.nio.http.HttpSession.processHttpBuffer
  [46] one.nio.http.HttpSession.processRead
  [47] one.nio.net.Session.process
  [48] one.nio.server.SelectorThread.run

--- 20358316 ns (0.30%), 2 samples
  [ 0] ipv4_conntrack_defrag?[nf_defrag_ipv4]_[k]
  [ 1] __ip_local_out_[k]
  [ 2] ip_local_out_[k]
  [ 3] __ip_queue_xmit_[k]
  [ 4] ip_queue_xmit_[k]
  [ 5] __tcp_transmit_skb_[k]
  [ 6] tcp_write_xmit_[k]
  [ 7] __tcp_push_pending_frames_[k]
  [ 8] tcp_push_[k]
  [ 9] tcp_sendmsg_locked_[k]
  [10] tcp_sendmsg_[k]
  [11] inet6_sendmsg_[k]
  [12] sock_sendmsg_[k]
  [13] __sys_sendto_[k]
  [14] __x64_sys_sendto_[k]
  [15] do_syscall_64_[k]
  [16] entry_SYSCALL_64_after_hwframe_[k]
  [17] __libc_send
  [18] one.nio.net.NativeSocket.write
  [19] one.nio.net.Session$ArrayQueueItem.write
  [20] one.nio.net.Session.write
  [21] one.nio.net.Session.write
  [22] one.nio.http.HttpSession.writeResponse
  [23] one.nio.http.HttpSession.sendResponse
  [24] RequestHandler2_status.handleRequest
  [25] one.nio.http.HttpServer.handleRequest
  [26] one.nio.http.HttpSession.handleParsedRequest
  [27] one.nio.http.HttpSession.processHttpBuffer
  [28] one.nio.http.HttpSession.processRead
  [29] one.nio.net.Session.process
  [30] one.nio.server.SelectorThread.run

--- 20356583 ns (0.30%), 2 samples
  [ 0] tcp_ack_update_rtt.isra.45_[k]
  [ 1] tcp_clean_rtx_queue_[k]
  [ 2] tcp_ack_[k]
  [ 3] tcp_rcv_established_[k]
  [ 4] tcp_v4_do_rcv_[k]
  [ 5] tcp_v4_rcv_[k]
  [ 6] ip_protocol_deliver_rcu_[k]
  [ 7] ip_local_deliver_finish_[k]
  [ 8] ip_local_deliver_[k]
  [ 9] ip_rcv_finish_[k]
  [10] ip_rcv_[k]
  [11] __netif_receive_skb_one_core_[k]
  [12] __netif_receive_skb_[k]
  [13] process_backlog_[k]
  [14] net_rx_action_[k]
  [15] __softirqentry_text_start_[k]
  [16] do_softirq_own_stack_[k]
  [17] do_softirq.part.20_[k]
  [18] __local_bh_enable_ip_[k]
  [19] ip_finish_output2_[k]
  [20] __ip_finish_output_[k]
  [21] ip_finish_output_[k]
  [22] ip_output_[k]
  [23] ip_local_out_[k]
  [24] __ip_queue_xmit_[k]
  [25] ip_queue_xmit_[k]
  [26] __tcp_transmit_skb_[k]
  [27] tcp_write_xmit_[k]
  [28] __tcp_push_pending_frames_[k]
  [29] tcp_push_[k]
  [30] tcp_sendmsg_locked_[k]
  [31] tcp_sendmsg_[k]
  [32] inet6_sendmsg_[k]
  [33] sock_sendmsg_[k]
  [34] __sys_sendto_[k]
  [35] __x64_sys_sendto_[k]
  [36] do_syscall_64_[k]
  [37] entry_SYSCALL_64_after_hwframe_[k]
  [38] __libc_send
  [39] one.nio.net.NativeSocket.write
  [40] one.nio.net.Session$ArrayQueueItem.write
  [41] one.nio.net.Session.write
  [42] one.nio.net.Session.write
  [43] one.nio.http.HttpSession.writeResponse
  [44] one.nio.http.HttpSession.sendResponse
  [45] RequestHandler2_status.handleRequest
  [46] one.nio.http.HttpServer.handleRequest
  [47] one.nio.http.HttpSession.handleParsedRequest
  [48] one.nio.http.HttpSession.processHttpBuffer
  [49] one.nio.http.HttpSession.processRead
  [50] one.nio.net.Session.process
  [51] one.nio.server.SelectorThread.run

--- 20355159 ns (0.30%), 2 samples
  [ 0] tcp_queue_rcv_[k]
  [ 1] tcp_rcv_established_[k]
  [ 2] tcp_v4_do_rcv_[k]
  [ 3] tcp_v4_rcv_[k]
  [ 4] ip_protocol_deliver_rcu_[k]
  [ 5] ip_local_deliver_finish_[k]
  [ 6] ip_local_deliver_[k]
  [ 7] ip_rcv_finish_[k]
  [ 8] ip_rcv_[k]
  [ 9] __netif_receive_skb_one_core_[k]
  [10] __netif_receive_skb_[k]
  [11] process_backlog_[k]
  [12] net_rx_action_[k]
  [13] __softirqentry_text_start_[k]
  [14] do_softirq_own_stack_[k]
  [15] do_softirq.part.20_[k]
  [16] __local_bh_enable_ip_[k]
  [17] ip_finish_output2_[k]
  [18] __ip_finish_output_[k]
  [19] ip_finish_output_[k]
  [20] ip_output_[k]
  [21] ip_local_out_[k]
  [22] __ip_queue_xmit_[k]
  [23] ip_queue_xmit_[k]
  [24] __tcp_transmit_skb_[k]
  [25] tcp_write_xmit_[k]
  [26] __tcp_push_pending_frames_[k]
  [27] tcp_push_[k]
  [28] tcp_sendmsg_locked_[k]
  [29] tcp_sendmsg_[k]
  [30] inet6_sendmsg_[k]
  [31] sock_sendmsg_[k]
  [32] __sys_sendto_[k]
  [33] __x64_sys_sendto_[k]
  [34] do_syscall_64_[k]
  [35] entry_SYSCALL_64_after_hwframe_[k]
  [36] __libc_send
  [37] one.nio.net.NativeSocket.write
  [38] one.nio.net.Session$ArrayQueueItem.write
  [39] one.nio.net.Session.write
  [40] one.nio.net.Session.write
  [41] one.nio.http.HttpSession.writeResponse
  [42] one.nio.http.HttpSession.sendResponse
  [43] RequestHandler2_status.handleRequest
  [44] one.nio.http.HttpServer.handleRequest
  [45] one.nio.http.HttpSession.handleParsedRequest
  [46] one.nio.http.HttpSession.processHttpBuffer
  [47] one.nio.http.HttpSession.processRead
  [48] one.nio.net.Session.process
  [49] one.nio.server.SelectorThread.run

--- 20353538 ns (0.30%), 2 samples
  [ 0] __alloc_skb_[k]
  [ 1] sk_stream_alloc_skb_[k]
  [ 2] tcp_sendmsg_locked_[k]
  [ 3] tcp_sendmsg_[k]
  [ 4] inet6_sendmsg_[k]
  [ 5] sock_sendmsg_[k]
  [ 6] __sys_sendto_[k]
  [ 7] __x64_sys_sendto_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __libc_send
  [11] one.nio.net.NativeSocket.write
  [12] one.nio.net.Session$ArrayQueueItem.write
  [13] one.nio.net.Session.write
  [14] one.nio.net.Session.write
  [15] one.nio.http.HttpSession.writeResponse
  [16] one.nio.http.HttpSession.sendResponse
  [17] RequestHandler2_status.handleRequest
  [18] one.nio.http.HttpServer.handleRequest
  [19] one.nio.http.HttpSession.handleParsedRequest
  [20] one.nio.http.HttpSession.processHttpBuffer
  [21] one.nio.http.HttpSession.processRead
  [22] one.nio.net.Session.process
  [23] one.nio.server.SelectorThread.run

--- 20353176 ns (0.30%), 2 samples
  [ 0] apparmor_socket_recvmsg_[k]
  [ 1] sock_recvmsg_[k]
  [ 2] __sys_recvfrom_[k]
  [ 3] __x64_sys_recvfrom_[k]
  [ 4] do_syscall_64_[k]
  [ 5] entry_SYSCALL_64_after_hwframe_[k]
  [ 6] __GI___recv
  [ 7] one.nio.net.NativeSocket.read
  [ 8] one.nio.net.Session.read
  [ 9] one.nio.http.HttpSession.processRead
  [10] one.nio.net.Session.process
  [11] one.nio.server.SelectorThread.run

--- 20352895 ns (0.30%), 2 samples
  [ 0] dst_release_[k]
  [ 1] skb_release_head_state_[k]
  [ 2] skb_release_all_[k]
  [ 3] __kfree_skb_[k]
  [ 4] tcp_recvmsg_[k]
  [ 5] inet6_recvmsg_[k]
  [ 6] sock_recvmsg_[k]
  [ 7] __sys_recvfrom_[k]
  [ 8] __x64_sys_recvfrom_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __GI___recv
  [12] one.nio.net.NativeSocket.read
  [13] one.nio.net.Session.read
  [14] one.nio.http.HttpSession.processRead
  [15] one.nio.net.Session.process
  [16] one.nio.server.SelectorThread.run

--- 20352881 ns (0.30%), 2 samples
  [ 0] tcp_cleanup_rbuf_[k]
  [ 1] tcp_recvmsg_[k]
  [ 2] inet6_recvmsg_[k]
  [ 3] sock_recvmsg_[k]
  [ 4] __sys_recvfrom_[k]
  [ 5] __x64_sys_recvfrom_[k]
  [ 6] do_syscall_64_[k]
  [ 7] entry_SYSCALL_64_after_hwframe_[k]
  [ 8] __GI___recv
  [ 9] one.nio.net.NativeSocket.read
  [10] one.nio.net.Session.read
  [11] one.nio.http.HttpSession.processRead
  [12] one.nio.net.Session.process
  [13] one.nio.server.SelectorThread.run

--- 20352571 ns (0.30%), 2 samples
  [ 0] nf_ct_get_tuple?[nf_conntrack]_[k]
  [ 1] nf_conntrack_in?[nf_conntrack]_[k]
  [ 2] ipv4_conntrack_local?[nf_conntrack]_[k]
  [ 3] nf_hook_slow_[k]
  [ 4] __ip_local_out_[k]
  [ 5] ip_local_out_[k]
  [ 6] __ip_queue_xmit_[k]
  [ 7] ip_queue_xmit_[k]
  [ 8] __tcp_transmit_skb_[k]
  [ 9] tcp_write_xmit_[k]
  [10] __tcp_push_pending_frames_[k]
  [11] tcp_push_[k]
  [12] tcp_sendmsg_locked_[k]
  [13] tcp_sendmsg_[k]
  [14] inet6_sendmsg_[k]
  [15] sock_sendmsg_[k]
  [16] __sys_sendto_[k]
  [17] __x64_sys_sendto_[k]
  [18] do_syscall_64_[k]
  [19] entry_SYSCALL_64_after_hwframe_[k]
  [20] __libc_send
  [21] one.nio.net.NativeSocket.write
  [22] one.nio.net.Session$ArrayQueueItem.write
  [23] one.nio.net.Session.write
  [24] one.nio.net.Session.write
  [25] one.nio.http.HttpSession.writeResponse
  [26] one.nio.http.HttpSession.sendResponse
  [27] RequestHandler2_status.handleRequest
  [28] one.nio.http.HttpServer.handleRequest
  [29] one.nio.http.HttpSession.handleParsedRequest
  [30] one.nio.http.HttpSession.processHttpBuffer
  [31] one.nio.http.HttpSession.processRead
  [32] one.nio.net.Session.process
  [33] one.nio.server.SelectorThread.run

--- 20351884 ns (0.30%), 2 samples
  [ 0] nf_ct_deliver_cached_events?[nf_conntrack]_[k]
  [ 1] ipv4_confirm?[nf_conntrack]_[k]
  [ 2] nf_hook_slow_[k]
  [ 3] ip_output_[k]
  [ 4] ip_local_out_[k]
  [ 5] __ip_queue_xmit_[k]
  [ 6] ip_queue_xmit_[k]
  [ 7] __tcp_transmit_skb_[k]
  [ 8] tcp_write_xmit_[k]
  [ 9] __tcp_push_pending_frames_[k]
  [10] tcp_push_[k]
  [11] tcp_sendmsg_locked_[k]
  [12] tcp_sendmsg_[k]
  [13] inet6_sendmsg_[k]
  [14] sock_sendmsg_[k]
  [15] __sys_sendto_[k]
  [16] __x64_sys_sendto_[k]
  [17] do_syscall_64_[k]
  [18] entry_SYSCALL_64_after_hwframe_[k]
  [19] __libc_send
  [20] one.nio.net.NativeSocket.write
  [21] one.nio.net.Session$ArrayQueueItem.write
  [22] one.nio.net.Session.write
  [23] one.nio.net.Session.write
  [24] one.nio.http.HttpSession.writeResponse
  [25] one.nio.http.HttpSession.sendResponse
  [26] RequestHandler2_status.handleRequest
  [27] one.nio.http.HttpServer.handleRequest
  [28] one.nio.http.HttpSession.handleParsedRequest
  [29] one.nio.http.HttpSession.processHttpBuffer
  [30] one.nio.http.HttpSession.processRead
  [31] one.nio.net.Session.process
  [32] one.nio.server.SelectorThread.run

--- 20351650 ns (0.30%), 2 samples
  [ 0] java.lang.StringLatin1.regionMatchesCI
  [ 1] java.lang.String.regionMatches
  [ 2] one.nio.http.Request.getHeader
  [ 3] one.nio.http.HttpSession.sendResponse
  [ 4] RequestHandler2_status.handleRequest
  [ 5] one.nio.http.HttpServer.handleRequest
  [ 6] one.nio.http.HttpSession.handleParsedRequest
  [ 7] one.nio.http.HttpSession.processHttpBuffer
  [ 8] one.nio.http.HttpSession.processRead
  [ 9] one.nio.net.Session.process
  [10] one.nio.server.SelectorThread.run

--- 20349168 ns (0.30%), 2 samples
  [ 0] tcp_event_new_data_sent_[k]
  [ 1] tcp_write_xmit_[k]
  [ 2] __tcp_push_pending_frames_[k]
  [ 3] tcp_push_[k]
  [ 4] tcp_sendmsg_locked_[k]
  [ 5] tcp_sendmsg_[k]
  [ 6] inet6_sendmsg_[k]
  [ 7] sock_sendmsg_[k]
  [ 8] __sys_sendto_[k]
  [ 9] __x64_sys_sendto_[k]
  [10] do_syscall_64_[k]
  [11] entry_SYSCALL_64_after_hwframe_[k]
  [12] __libc_send
  [13] one.nio.net.NativeSocket.write
  [14] one.nio.net.Session$ArrayQueueItem.write
  [15] one.nio.net.Session.write
  [16] one.nio.net.Session.write
  [17] one.nio.http.HttpSession.writeResponse
  [18] one.nio.http.HttpSession.sendResponse
  [19] RequestHandler2_status.handleRequest
  [20] one.nio.http.HttpServer.handleRequest
  [21] one.nio.http.HttpSession.handleParsedRequest
  [22] one.nio.http.HttpSession.processHttpBuffer
  [23] one.nio.http.HttpSession.processRead
  [24] one.nio.net.Session.process
  [25] one.nio.server.SelectorThread.run

--- 20348595 ns (0.30%), 2 samples
  [ 0] ktime_get_seconds_[k]
  [ 1] tcp_v4_do_rcv_[k]
  [ 2] tcp_v4_rcv_[k]
  [ 3] ip_protocol_deliver_rcu_[k]
  [ 4] ip_local_deliver_finish_[k]
  [ 5] ip_local_deliver_[k]
  [ 6] ip_rcv_finish_[k]
  [ 7] ip_rcv_[k]
  [ 8] __netif_receive_skb_one_core_[k]
  [ 9] __netif_receive_skb_[k]
  [10] process_backlog_[k]
  [11] net_rx_action_[k]
  [12] __softirqentry_text_start_[k]
  [13] do_softirq_own_stack_[k]
  [14] do_softirq.part.20_[k]
  [15] __local_bh_enable_ip_[k]
  [16] ip_finish_output2_[k]
  [17] __ip_finish_output_[k]
  [18] ip_finish_output_[k]
  [19] ip_output_[k]
  [20] ip_local_out_[k]
  [21] __ip_queue_xmit_[k]
  [22] ip_queue_xmit_[k]
  [23] __tcp_transmit_skb_[k]
  [24] tcp_write_xmit_[k]
  [25] __tcp_push_pending_frames_[k]
  [26] tcp_push_[k]
  [27] tcp_sendmsg_locked_[k]
  [28] tcp_sendmsg_[k]
  [29] inet6_sendmsg_[k]
  [30] sock_sendmsg_[k]
  [31] __sys_sendto_[k]
  [32] __x64_sys_sendto_[k]
  [33] do_syscall_64_[k]
  [34] entry_SYSCALL_64_after_hwframe_[k]
  [35] __libc_send
  [36] one.nio.net.NativeSocket.write
  [37] one.nio.net.Session$ArrayQueueItem.write
  [38] one.nio.net.Session.write
  [39] one.nio.net.Session.write
  [40] one.nio.http.HttpSession.writeResponse
  [41] one.nio.http.HttpSession.sendResponse
  [42] RequestHandler2_status.handleRequest
  [43] one.nio.http.HttpServer.handleRequest
  [44] one.nio.http.HttpSession.handleParsedRequest
  [45] one.nio.http.HttpSession.processHttpBuffer
  [46] one.nio.http.HttpSession.processRead
  [47] one.nio.net.Session.process
  [48] one.nio.server.SelectorThread.run

--- 20341916 ns (0.30%), 2 samples
  [ 0] Java_one_nio_net_NativeSocket_write
  [ 1] one.nio.net.NativeSocket.write
  [ 2] one.nio.net.Session$ArrayQueueItem.write
  [ 3] one.nio.net.Session.write
  [ 4] one.nio.net.Session.write
  [ 5] one.nio.http.HttpSession.writeResponse
  [ 6] one.nio.http.HttpSession.sendResponse
  [ 7] RequestHandler2_status.handleRequest
  [ 8] one.nio.http.HttpServer.handleRequest
  [ 9] one.nio.http.HttpSession.handleParsedRequest
  [10] one.nio.http.HttpSession.processHttpBuffer
  [11] one.nio.http.HttpSession.processRead
  [12] one.nio.net.Session.process
  [13] one.nio.server.SelectorThread.run

--- 20340133 ns (0.30%), 2 samples
  [ 0] __libc_disable_asynccancel
  [ 1] one.nio.net.NativeSocket.write
  [ 2] one.nio.net.Session$ArrayQueueItem.write
  [ 3] one.nio.net.Session.write
  [ 4] one.nio.net.Session.write
  [ 5] one.nio.http.HttpSession.writeResponse
  [ 6] one.nio.http.HttpSession.sendResponse
  [ 7] RequestHandler2_status.handleRequest
  [ 8] one.nio.http.HttpServer.handleRequest
  [ 9] one.nio.http.HttpSession.handleParsedRequest
  [10] one.nio.http.HttpSession.processHttpBuffer
  [11] one.nio.http.HttpSession.processRead
  [12] one.nio.net.Session.process
  [13] one.nio.server.SelectorThread.run

--- 20336896 ns (0.30%), 2 samples
  [ 0] ipv4_conntrack_defrag?[nf_defrag_ipv4]_[k]
  [ 1] ip_rcv_[k]
  [ 2] __netif_receive_skb_one_core_[k]
  [ 3] __netif_receive_skb_[k]
  [ 4] process_backlog_[k]
  [ 5] net_rx_action_[k]
  [ 6] __softirqentry_text_start_[k]
  [ 7] do_softirq_own_stack_[k]
  [ 8] do_softirq.part.20_[k]
  [ 9] __local_bh_enable_ip_[k]
  [10] ip_finish_output2_[k]
  [11] __ip_finish_output_[k]
  [12] ip_finish_output_[k]
  [13] ip_output_[k]
  [14] ip_local_out_[k]
  [15] __ip_queue_xmit_[k]
  [16] ip_queue_xmit_[k]
  [17] __tcp_transmit_skb_[k]
  [18] tcp_write_xmit_[k]
  [19] __tcp_push_pending_frames_[k]
  [20] tcp_push_[k]
  [21] tcp_sendmsg_locked_[k]
  [22] tcp_sendmsg_[k]
  [23] inet6_sendmsg_[k]
  [24] sock_sendmsg_[k]
  [25] __sys_sendto_[k]
  [26] __x64_sys_sendto_[k]
  [27] do_syscall_64_[k]
  [28] entry_SYSCALL_64_after_hwframe_[k]
  [29] __libc_send
  [30] one.nio.net.NativeSocket.write
  [31] one.nio.net.Session$ArrayQueueItem.write
  [32] one.nio.net.Session.write
  [33] one.nio.net.Session.write
  [34] one.nio.http.HttpSession.writeResponse
  [35] one.nio.http.HttpSession.sendResponse
  [36] RequestHandler2_status.handleRequest
  [37] one.nio.http.HttpServer.handleRequest
  [38] one.nio.http.HttpSession.handleParsedRequest
  [39] one.nio.http.HttpSession.processHttpBuffer
  [40] one.nio.http.HttpSession.processRead
  [41] one.nio.net.Session.process
  [42] one.nio.server.SelectorThread.run

--- 20323079 ns (0.30%), 2 samples
  [ 0] __x64_sys_recvfrom_[k]
  [ 1] entry_SYSCALL_64_after_hwframe_[k]
  [ 2] __GI___recv
  [ 3] one.nio.net.NativeSocket.read
  [ 4] one.nio.net.Session.read
  [ 5] one.nio.http.HttpSession.processRead
  [ 6] one.nio.net.Session.process
  [ 7] one.nio.server.SelectorThread.run

--- 10441599 ns (0.15%), 1 sample
  [ 0] schedule_hrtimeout_range_clock_[k]
  [ 1] schedule_hrtimeout_range_[k]
  [ 2] ep_poll_[k]
  [ 3] do_epoll_wait_[k]
  [ 4] __x64_sys_epoll_wait_[k]
  [ 5] do_syscall_64_[k]
  [ 6] entry_SYSCALL_64_after_hwframe_[k]
  [ 7] epoll_wait
  [ 8] [unknown]
  [ 9] one.nio.net.NativeSelector.epollWait
  [10] one.nio.net.NativeSelector.select
  [11] one.nio.server.SelectorThread.run

--- 10311278 ns (0.15%), 1 sample
  [ 0] __ip_finish_output_[k]
  [ 1] ip_finish_output_[k]
  [ 2] ip_output_[k]
  [ 3] ip_local_out_[k]
  [ 4] __ip_queue_xmit_[k]
  [ 5] ip_queue_xmit_[k]
  [ 6] __tcp_transmit_skb_[k]
  [ 7] tcp_write_xmit_[k]
  [ 8] __tcp_push_pending_frames_[k]
  [ 9] tcp_push_[k]
  [10] tcp_sendmsg_locked_[k]
  [11] tcp_sendmsg_[k]
  [12] inet6_sendmsg_[k]
  [13] sock_sendmsg_[k]
  [14] __sys_sendto_[k]
  [15] __x64_sys_sendto_[k]
  [16] do_syscall_64_[k]
  [17] entry_SYSCALL_64_after_hwframe_[k]
  [18] __libc_send
  [19] one.nio.net.NativeSocket.write
  [20] one.nio.net.Session$ArrayQueueItem.write
  [21] one.nio.net.Session.write
  [22] one.nio.net.Session.write
  [23] one.nio.http.HttpSession.writeResponse
  [24] one.nio.http.HttpSession.sendResponse
  [25] RequestHandler2_status.handleRequest
  [26] one.nio.http.HttpServer.handleRequest
  [27] one.nio.http.HttpSession.handleParsedRequest
  [28] one.nio.http.HttpSession.processHttpBuffer
  [29] one.nio.http.HttpSession.processRead
  [30] one.nio.net.Session.process
  [31] one.nio.server.SelectorThread.run

--- 10234327 ns (0.15%), 1 sample
  [ 0] ep_send_events_proc_[k]
  [ 1] ep_scan_ready_list.constprop.20_[k]
  [ 2] ep_poll_[k]
  [ 3] do_epoll_wait_[k]
  [ 4] __x64_sys_epoll_wait_[k]
  [ 5] do_syscall_64_[k]
  [ 6] entry_SYSCALL_64_after_hwframe_[k]
  [ 7] epoll_wait
  [ 8] [unknown]
  [ 9] one.nio.net.NativeSelector.epollWait
  [10] one.nio.net.NativeSelector.select
  [11] one.nio.server.SelectorThread.run

--- 10226763 ns (0.15%), 1 sample
  [ 0] __kfree_skb_[k]
  [ 1] tcp_ack_[k]
  [ 2] tcp_rcv_established_[k]
  [ 3] tcp_v4_do_rcv_[k]
  [ 4] tcp_v4_rcv_[k]
  [ 5] ip_protocol_deliver_rcu_[k]
  [ 6] ip_local_deliver_finish_[k]
  [ 7] ip_local_deliver_[k]
  [ 8] ip_rcv_finish_[k]
  [ 9] ip_rcv_[k]
  [10] __netif_receive_skb_one_core_[k]
  [11] __netif_receive_skb_[k]
  [12] process_backlog_[k]
  [13] net_rx_action_[k]
  [14] __softirqentry_text_start_[k]
  [15] do_softirq_own_stack_[k]
  [16] do_softirq.part.20_[k]
  [17] __local_bh_enable_ip_[k]
  [18] ip_finish_output2_[k]
  [19] __ip_finish_output_[k]
  [20] ip_finish_output_[k]
  [21] ip_output_[k]
  [22] ip_local_out_[k]
  [23] __ip_queue_xmit_[k]
  [24] ip_queue_xmit_[k]
  [25] __tcp_transmit_skb_[k]
  [26] tcp_write_xmit_[k]
  [27] __tcp_push_pending_frames_[k]
  [28] tcp_push_[k]
  [29] tcp_sendmsg_locked_[k]
  [30] tcp_sendmsg_[k]
  [31] inet6_sendmsg_[k]
  [32] sock_sendmsg_[k]
  [33] __sys_sendto_[k]
  [34] __x64_sys_sendto_[k]
  [35] do_syscall_64_[k]
  [36] entry_SYSCALL_64_after_hwframe_[k]
  [37] __libc_send
  [38] one.nio.net.NativeSocket.write
  [39] one.nio.net.Session$ArrayQueueItem.write
  [40] one.nio.net.Session.write
  [41] one.nio.net.Session.write
  [42] one.nio.http.HttpSession.writeResponse
  [43] one.nio.http.HttpSession.sendResponse
  [44] RequestHandler2_status.handleRequest
  [45] one.nio.http.HttpServer.handleRequest
  [46] one.nio.http.HttpSession.handleParsedRequest
  [47] one.nio.http.HttpSession.processHttpBuffer
  [48] one.nio.http.HttpSession.processRead
  [49] one.nio.net.Session.process
  [50] one.nio.server.SelectorThread.run

--- 10226086 ns (0.15%), 1 sample
  [ 0] ThreadInVMfromNative::~ThreadInVMfromNative()
  [ 1] jni_SetByteArrayRegion
  [ 2] Java_one_nio_net_NativeSocket_read
  [ 3] one.nio.net.NativeSocket.read
  [ 4] one.nio.net.Session.read
  [ 5] one.nio.http.HttpSession.processRead
  [ 6] one.nio.net.Session.process
  [ 7] one.nio.server.SelectorThread.run

--- 10223886 ns (0.15%), 1 sample
  [ 0] tcp_schedule_loss_probe_[k]
  [ 1] tcp_rcv_established_[k]
  [ 2] tcp_v4_do_rcv_[k]
  [ 3] tcp_v4_rcv_[k]
  [ 4] ip_protocol_deliver_rcu_[k]
  [ 5] ip_local_deliver_finish_[k]
  [ 6] ip_local_deliver_[k]
  [ 7] ip_rcv_finish_[k]
  [ 8] ip_rcv_[k]
  [ 9] __netif_receive_skb_one_core_[k]
  [10] __netif_receive_skb_[k]
  [11] process_backlog_[k]
  [12] net_rx_action_[k]
  [13] __softirqentry_text_start_[k]
  [14] do_softirq_own_stack_[k]
  [15] do_softirq.part.20_[k]
  [16] __local_bh_enable_ip_[k]
  [17] ip_finish_output2_[k]
  [18] __ip_finish_output_[k]
  [19] ip_finish_output_[k]
  [20] ip_output_[k]
  [21] ip_local_out_[k]
  [22] __ip_queue_xmit_[k]
  [23] ip_queue_xmit_[k]
  [24] __tcp_transmit_skb_[k]
  [25] tcp_write_xmit_[k]
  [26] __tcp_push_pending_frames_[k]
  [27] tcp_push_[k]
  [28] tcp_sendmsg_locked_[k]
  [29] tcp_sendmsg_[k]
  [30] inet6_sendmsg_[k]
  [31] sock_sendmsg_[k]
  [32] __sys_sendto_[k]
  [33] __x64_sys_sendto_[k]
  [34] do_syscall_64_[k]
  [35] entry_SYSCALL_64_after_hwframe_[k]
  [36] __libc_send
  [37] one.nio.net.NativeSocket.write
  [38] one.nio.net.Session$ArrayQueueItem.write
  [39] one.nio.net.Session.write
  [40] one.nio.net.Session.write
  [41] one.nio.http.HttpSession.writeResponse
  [42] one.nio.http.HttpSession.sendResponse
  [43] RequestHandler2_status.handleRequest
  [44] one.nio.http.HttpServer.handleRequest
  [45] one.nio.http.HttpSession.handleParsedRequest
  [46] one.nio.http.HttpSession.processHttpBuffer
  [47] one.nio.http.HttpSession.processRead
  [48] one.nio.net.Session.process
  [49] one.nio.server.SelectorThread.run

--- 10223079 ns (0.15%), 1 sample
  [ 0] tcp_current_mss_[k]
  [ 1] tcp_sendmsg_locked_[k]
  [ 2] tcp_sendmsg_[k]
  [ 3] inet6_sendmsg_[k]
  [ 4] sock_sendmsg_[k]
  [ 5] __sys_sendto_[k]
  [ 6] __x64_sys_sendto_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] __libc_send
  [10] one.nio.net.NativeSocket.write
  [11] one.nio.net.Session$ArrayQueueItem.write
  [12] one.nio.net.Session.write
  [13] one.nio.net.Session.write
  [14] one.nio.http.HttpSession.writeResponse
  [15] one.nio.http.HttpSession.sendResponse
  [16] RequestHandler2_status.handleRequest
  [17] one.nio.http.HttpServer.handleRequest
  [18] one.nio.http.HttpSession.handleParsedRequest
  [19] one.nio.http.HttpSession.processHttpBuffer
  [20] one.nio.http.HttpSession.processRead
  [21] one.nio.net.Session.process
  [22] one.nio.server.SelectorThread.run

--- 10222025 ns (0.15%), 1 sample
  [ 0] __sched_text_start_[k]
  [ 1] schedule_[k]
  [ 2] schedule_hrtimeout_range_clock_[k]
  [ 3] schedule_hrtimeout_range_[k]
  [ 4] ep_poll_[k]
  [ 5] do_epoll_wait_[k]
  [ 6] __x64_sys_epoll_wait_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] epoll_wait
  [10] [unknown]
  [11] one.nio.net.NativeSelector.epollWait
  [12] one.nio.net.NativeSelector.select
  [13] one.nio.server.SelectorThread.run

--- 10221632 ns (0.15%), 1 sample
  [ 0] __kmalloc_node_track_caller_[k]
  [ 1] __alloc_skb_[k]
  [ 2] sk_stream_alloc_skb_[k]
  [ 3] tcp_sendmsg_locked_[k]
  [ 4] tcp_sendmsg_[k]
  [ 5] inet6_sendmsg_[k]
  [ 6] sock_sendmsg_[k]
  [ 7] __sys_sendto_[k]
  [ 8] __x64_sys_sendto_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __libc_send
  [12] one.nio.net.NativeSocket.write
  [13] one.nio.net.Session$ArrayQueueItem.write
  [14] one.nio.net.Session.write
  [15] one.nio.net.Session.write
  [16] one.nio.http.HttpSession.writeResponse
  [17] one.nio.http.HttpSession.sendResponse
  [18] RequestHandler2_status.handleRequest
  [19] one.nio.http.HttpServer.handleRequest
  [20] one.nio.http.HttpSession.handleParsedRequest
  [21] one.nio.http.HttpSession.processHttpBuffer
  [22] one.nio.http.HttpSession.processRead
  [23] one.nio.net.Session.process
  [24] one.nio.server.SelectorThread.run

--- 10220769 ns (0.15%), 1 sample
  [ 0] tcp_push_[k]
  [ 1] tcp_sendmsg_locked_[k]
  [ 2] tcp_sendmsg_[k]
  [ 3] inet6_sendmsg_[k]
  [ 4] sock_sendmsg_[k]
  [ 5] __sys_sendto_[k]
  [ 6] __x64_sys_sendto_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] __libc_send
  [10] one.nio.net.NativeSocket.write
  [11] one.nio.net.Session$ArrayQueueItem.write
  [12] one.nio.net.Session.write
  [13] one.nio.net.Session.write
  [14] one.nio.http.HttpSession.writeResponse
  [15] one.nio.http.HttpSession.sendResponse
  [16] RequestHandler2_status.handleRequest
  [17] one.nio.http.HttpServer.handleRequest
  [18] one.nio.http.HttpSession.handleParsedRequest
  [19] one.nio.http.HttpSession.processHttpBuffer
  [20] one.nio.http.HttpSession.processRead
  [21] one.nio.net.Session.process
  [22] one.nio.server.SelectorThread.run

--- 10219821 ns (0.15%), 1 sample
  [ 0] read_tsc_[k]
  [ 1] ktime_get_[k]
  [ 2] tcp_write_xmit_[k]
  [ 3] __tcp_push_pending_frames_[k]
  [ 4] tcp_push_[k]
  [ 5] tcp_sendmsg_locked_[k]
  [ 6] tcp_sendmsg_[k]
  [ 7] inet6_sendmsg_[k]
  [ 8] sock_sendmsg_[k]
  [ 9] __sys_sendto_[k]
  [10] __x64_sys_sendto_[k]
  [11] do_syscall_64_[k]
  [12] entry_SYSCALL_64_after_hwframe_[k]
  [13] __libc_send
  [14] one.nio.net.NativeSocket.write
  [15] one.nio.net.Session$ArrayQueueItem.write
  [16] one.nio.net.Session.write
  [17] one.nio.net.Session.write
  [18] one.nio.http.HttpSession.writeResponse
  [19] one.nio.http.HttpSession.sendResponse
  [20] RequestHandler2_status.handleRequest
  [21] one.nio.http.HttpServer.handleRequest
  [22] one.nio.http.HttpSession.handleParsedRequest
  [23] one.nio.http.HttpSession.processHttpBuffer
  [24] one.nio.http.HttpSession.processRead
  [25] one.nio.net.Session.process
  [26] one.nio.server.SelectorThread.run

--- 10219672 ns (0.15%), 1 sample
  [ 0] tcp_rate_skb_delivered_[k]
  [ 1] tcp_ack_[k]
  [ 2] tcp_rcv_established_[k]
  [ 3] tcp_v4_do_rcv_[k]
  [ 4] tcp_v4_rcv_[k]
  [ 5] ip_protocol_deliver_rcu_[k]
  [ 6] ip_local_deliver_finish_[k]
  [ 7] ip_local_deliver_[k]
  [ 8] ip_rcv_finish_[k]
  [ 9] ip_rcv_[k]
  [10] __netif_receive_skb_one_core_[k]
  [11] __netif_receive_skb_[k]
  [12] process_backlog_[k]
  [13] net_rx_action_[k]
  [14] __softirqentry_text_start_[k]
  [15] do_softirq_own_stack_[k]
  [16] do_softirq.part.20_[k]
  [17] __local_bh_enable_ip_[k]
  [18] ip_finish_output2_[k]
  [19] __ip_finish_output_[k]
  [20] ip_finish_output_[k]
  [21] ip_output_[k]
  [22] ip_local_out_[k]
  [23] __ip_queue_xmit_[k]
  [24] ip_queue_xmit_[k]
  [25] __tcp_transmit_skb_[k]
  [26] tcp_write_xmit_[k]
  [27] __tcp_push_pending_frames_[k]
  [28] tcp_push_[k]
  [29] tcp_sendmsg_locked_[k]
  [30] tcp_sendmsg_[k]
  [31] inet6_sendmsg_[k]
  [32] sock_sendmsg_[k]
  [33] __sys_sendto_[k]
  [34] __x64_sys_sendto_[k]
  [35] do_syscall_64_[k]
  [36] entry_SYSCALL_64_after_hwframe_[k]
  [37] __libc_send
  [38] one.nio.net.NativeSocket.write
  [39] one.nio.net.Session$ArrayQueueItem.write
  [40] one.nio.net.Session.write
  [41] one.nio.net.Session.write
  [42] one.nio.http.HttpSession.writeResponse
  [43] one.nio.http.HttpSession.sendResponse
  [44] RequestHandler2_status.handleRequest
  [45] one.nio.http.HttpServer.handleRequest
  [46] one.nio.http.HttpSession.handleParsedRequest
  [47] one.nio.http.HttpSession.processHttpBuffer
  [48] one.nio.http.HttpSession.processRead
  [49] one.nio.net.Session.process
  [50] one.nio.server.SelectorThread.run

--- 10218681 ns (0.15%), 1 sample
  [ 0] one.nio.http.HttpSession.writeResponse
  [ 1] one.nio.http.HttpSession.sendResponse
  [ 2] RequestHandler2_status.handleRequest
  [ 3] one.nio.http.HttpServer.handleRequest
  [ 4] one.nio.http.HttpSession.handleParsedRequest
  [ 5] one.nio.http.HttpSession.processHttpBuffer
  [ 6] one.nio.http.HttpSession.processRead
  [ 7] one.nio.net.Session.process
  [ 8] one.nio.server.SelectorThread.run

--- 10218053 ns (0.15%), 1 sample
  [ 0] __virt_addr_valid_[k]
  [ 1] simple_copy_to_iter_[k]
  [ 2] __skb_datagram_iter_[k]
  [ 3] skb_copy_datagram_iter_[k]
  [ 4] tcp_recvmsg_[k]
  [ 5] inet6_recvmsg_[k]
  [ 6] sock_recvmsg_[k]
  [ 7] __sys_recvfrom_[k]
  [ 8] __x64_sys_recvfrom_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __GI___recv
  [12] one.nio.net.NativeSocket.read
  [13] one.nio.net.Session.read
  [14] one.nio.http.HttpSession.processRead
  [15] one.nio.net.Session.process
  [16] one.nio.server.SelectorThread.run

--- 10216889 ns (0.15%), 1 sample
  [ 0] kfree_skbmem_[k]
  [ 1] __kfree_skb_[k]
  [ 2] tcp_recvmsg_[k]
  [ 3] inet6_recvmsg_[k]
  [ 4] sock_recvmsg_[k]
  [ 5] __sys_recvfrom_[k]
  [ 6] __x64_sys_recvfrom_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] __GI___recv
  [10] one.nio.net.NativeSocket.read
  [11] one.nio.net.Session.read
  [12] one.nio.http.HttpSession.processRead
  [13] one.nio.net.Session.process
  [14] one.nio.server.SelectorThread.run

--- 10216830 ns (0.15%), 1 sample
  [ 0] ip_rcv_[k]
  [ 1] __netif_receive_skb_[k]
  [ 2] process_backlog_[k]
  [ 3] net_rx_action_[k]
  [ 4] __softirqentry_text_start_[k]
  [ 5] do_softirq_own_stack_[k]
  [ 6] do_softirq.part.20_[k]
  [ 7] __local_bh_enable_ip_[k]
  [ 8] ip_finish_output2_[k]
  [ 9] __ip_finish_output_[k]
  [10] ip_finish_output_[k]
  [11] ip_output_[k]
  [12] ip_local_out_[k]
  [13] __ip_queue_xmit_[k]
  [14] ip_queue_xmit_[k]
  [15] __tcp_transmit_skb_[k]
  [16] tcp_write_xmit_[k]
  [17] __tcp_push_pending_frames_[k]
  [18] tcp_push_[k]
  [19] tcp_sendmsg_locked_[k]
  [20] tcp_sendmsg_[k]
  [21] inet6_sendmsg_[k]
  [22] sock_sendmsg_[k]
  [23] __sys_sendto_[k]
  [24] __x64_sys_sendto_[k]
  [25] do_syscall_64_[k]
  [26] entry_SYSCALL_64_after_hwframe_[k]
  [27] __libc_send
  [28] one.nio.net.NativeSocket.write
  [29] one.nio.net.Session$ArrayQueueItem.write
  [30] one.nio.net.Session.write
  [31] one.nio.net.Session.write
  [32] one.nio.http.HttpSession.writeResponse
  [33] one.nio.http.HttpSession.sendResponse
  [34] RequestHandler2_status.handleRequest
  [35] one.nio.http.HttpServer.handleRequest
  [36] one.nio.http.HttpSession.handleParsedRequest
  [37] one.nio.http.HttpSession.processHttpBuffer
  [38] one.nio.http.HttpSession.processRead
  [39] one.nio.net.Session.process
  [40] one.nio.server.SelectorThread.run

--- 10216297 ns (0.15%), 1 sample
  [ 0] java.lang.StringLatin1.hashCode
  [ 1] java.lang.String.hashCode
  [ 2] java.util.HashMap.hash
  [ 3] java.util.HashMap.get
  [ 4] one.nio.http.PathMapper.find
  [ 5] one.nio.http.HttpServer.handleRequest
  [ 6] one.nio.http.HttpSession.handleParsedRequest
  [ 7] one.nio.http.HttpSession.processHttpBuffer
  [ 8] one.nio.http.HttpSession.processRead
  [ 9] one.nio.net.Session.process
  [10] one.nio.server.SelectorThread.run

--- 10216282 ns (0.15%), 1 sample
  [ 0] __fget_light_[k]
  [ 1] sockfd_lookup_light_[k]
  [ 2] __sys_recvfrom_[k]
  [ 3] __x64_sys_recvfrom_[k]
  [ 4] do_syscall_64_[k]
  [ 5] entry_SYSCALL_64_after_hwframe_[k]
  [ 6] __GI___recv
  [ 7] one.nio.net.NativeSocket.read
  [ 8] one.nio.net.Session.read
  [ 9] one.nio.http.HttpSession.processRead
  [10] one.nio.net.Session.process
  [11] one.nio.server.SelectorThread.run

--- 10216051 ns (0.15%), 1 sample
  [ 0] _raw_write_lock_irq_[k]
  [ 1] ep_scan_ready_list.constprop.20_[k]
  [ 2] ep_poll_[k]
  [ 3] do_epoll_wait_[k]
  [ 4] __x64_sys_epoll_wait_[k]
  [ 5] do_syscall_64_[k]
  [ 6] entry_SYSCALL_64_after_hwframe_[k]
  [ 7] epoll_wait
  [ 8] [unknown]
  [ 9] one.nio.net.NativeSelector.epollWait
  [10] one.nio.net.NativeSelector.select
  [11] one.nio.server.SelectorThread.run

--- 10215640 ns (0.15%), 1 sample
  [ 0] skb_network_protocol_[k]
  [ 1] validate_xmit_skb_[k]
  [ 2] __dev_queue_xmit_[k]
  [ 3] dev_queue_xmit_[k]
  [ 4] ip_finish_output2_[k]
  [ 5] __ip_finish_output_[k]
  [ 6] ip_finish_output_[k]
  [ 7] ip_output_[k]
  [ 8] ip_local_out_[k]
  [ 9] __ip_queue_xmit_[k]
  [10] ip_queue_xmit_[k]
  [11] __tcp_transmit_skb_[k]
  [12] tcp_write_xmit_[k]
  [13] __tcp_push_pending_frames_[k]
  [14] tcp_push_[k]
  [15] tcp_sendmsg_locked_[k]
  [16] tcp_sendmsg_[k]
  [17] inet6_sendmsg_[k]
  [18] sock_sendmsg_[k]
  [19] __sys_sendto_[k]
  [20] __x64_sys_sendto_[k]
  [21] do_syscall_64_[k]
  [22] entry_SYSCALL_64_after_hwframe_[k]
  [23] __libc_send
  [24] one.nio.net.NativeSocket.write
  [25] one.nio.net.Session$ArrayQueueItem.write
  [26] one.nio.net.Session.write
  [27] one.nio.net.Session.write
  [28] one.nio.http.HttpSession.writeResponse
  [29] one.nio.http.HttpSession.sendResponse
  [30] RequestHandler2_status.handleRequest
  [31] one.nio.http.HttpServer.handleRequest
  [32] one.nio.http.HttpSession.handleParsedRequest
  [33] one.nio.http.HttpSession.processHttpBuffer
  [34] one.nio.http.HttpSession.processRead
  [35] one.nio.net.Session.process
  [36] one.nio.server.SelectorThread.run

--- 10215628 ns (0.15%), 1 sample
  [ 0] __netif_receive_skb_one_core_[k]
  [ 1] process_backlog_[k]
  [ 2] net_rx_action_[k]
  [ 3] __softirqentry_text_start_[k]
  [ 4] do_softirq_own_stack_[k]
  [ 5] do_softirq.part.20_[k]
  [ 6] __local_bh_enable_ip_[k]
  [ 7] ip_finish_output2_[k]
  [ 8] __ip_finish_output_[k]
  [ 9] ip_finish_output_[k]
  [10] ip_output_[k]
  [11] ip_local_out_[k]
  [12] __ip_queue_xmit_[k]
  [13] ip_queue_xmit_[k]
  [14] __tcp_transmit_skb_[k]
  [15] tcp_write_xmit_[k]
  [16] __tcp_push_pending_frames_[k]
  [17] tcp_push_[k]
  [18] tcp_sendmsg_locked_[k]
  [19] tcp_sendmsg_[k]
  [20] inet6_sendmsg_[k]
  [21] sock_sendmsg_[k]
  [22] __sys_sendto_[k]
  [23] __x64_sys_sendto_[k]
  [24] do_syscall_64_[k]
  [25] entry_SYSCALL_64_after_hwframe_[k]
  [26] __libc_send
  [27] one.nio.net.NativeSocket.write
  [28] one.nio.net.Session$ArrayQueueItem.write
  [29] one.nio.net.Session.write
  [30] one.nio.net.Session.write
  [31] one.nio.http.HttpSession.writeResponse
  [32] one.nio.http.HttpSession.sendResponse
  [33] RequestHandler2_status.handleRequest
  [34] one.nio.http.HttpServer.handleRequest
  [35] one.nio.http.HttpSession.handleParsedRequest
  [36] one.nio.http.HttpSession.processHttpBuffer
  [37] one.nio.http.HttpSession.processRead
  [38] one.nio.net.Session.process
  [39] one.nio.server.SelectorThread.run

--- 10214458 ns (0.15%), 1 sample
  [ 0] iptable_filter_hook?[iptable_filter]_[k]
  [ 1] nf_hook_slow_[k]
  [ 2] __ip_local_out_[k]
  [ 3] ip_local_out_[k]
  [ 4] __ip_queue_xmit_[k]
  [ 5] ip_queue_xmit_[k]
  [ 6] __tcp_transmit_skb_[k]
  [ 7] tcp_write_xmit_[k]
  [ 8] __tcp_push_pending_frames_[k]
  [ 9] tcp_push_[k]
  [10] tcp_sendmsg_locked_[k]
  [11] tcp_sendmsg_[k]
  [12] inet6_sendmsg_[k]
  [13] sock_sendmsg_[k]
  [14] __sys_sendto_[k]
  [15] __x64_sys_sendto_[k]
  [16] do_syscall_64_[k]
  [17] entry_SYSCALL_64_after_hwframe_[k]
  [18] __libc_send
  [19] one.nio.net.NativeSocket.write
  [20] one.nio.net.Session$ArrayQueueItem.write
  [21] one.nio.net.Session.write
  [22] one.nio.net.Session.write
  [23] one.nio.http.HttpSession.writeResponse
  [24] one.nio.http.HttpSession.sendResponse
  [25] RequestHandler2_status.handleRequest
  [26] one.nio.http.HttpServer.handleRequest
  [27] one.nio.http.HttpSession.handleParsedRequest
  [28] one.nio.http.HttpSession.processHttpBuffer
  [29] one.nio.http.HttpSession.processRead
  [30] one.nio.net.Session.process
  [31] one.nio.server.SelectorThread.run

--- 10214003 ns (0.15%), 1 sample
  [ 0] tcp_event_new_data_sent_[k]
  [ 1] __tcp_push_pending_frames_[k]
  [ 2] tcp_push_[k]
  [ 3] tcp_sendmsg_locked_[k]
  [ 4] tcp_sendmsg_[k]
  [ 5] inet6_sendmsg_[k]
  [ 6] sock_sendmsg_[k]
  [ 7] __sys_sendto_[k]
  [ 8] __x64_sys_sendto_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __libc_send
  [12] one.nio.net.NativeSocket.write
  [13] one.nio.net.Session$ArrayQueueItem.write
  [14] one.nio.net.Session.write
  [15] one.nio.net.Session.write
  [16] one.nio.http.HttpSession.writeResponse
  [17] one.nio.http.HttpSession.sendResponse
  [18] RequestHandler2_status.handleRequest
  [19] one.nio.http.HttpServer.handleRequest
  [20] one.nio.http.HttpSession.handleParsedRequest
  [21] one.nio.http.HttpSession.processHttpBuffer
  [22] one.nio.http.HttpSession.processRead
  [23] one.nio.net.Session.process
  [24] one.nio.server.SelectorThread.run

--- 10213884 ns (0.15%), 1 sample
  [ 0] __tcp_transmit_skb_[k]
  [ 1] __tcp_push_pending_frames_[k]
  [ 2] tcp_push_[k]
  [ 3] tcp_sendmsg_locked_[k]
  [ 4] tcp_sendmsg_[k]
  [ 5] inet6_sendmsg_[k]
  [ 6] sock_sendmsg_[k]
  [ 7] __sys_sendto_[k]
  [ 8] __x64_sys_sendto_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __libc_send
  [12] one.nio.net.NativeSocket.write
  [13] one.nio.net.Session$ArrayQueueItem.write
  [14] one.nio.net.Session.write
  [15] one.nio.net.Session.write
  [16] one.nio.http.HttpSession.writeResponse
  [17] one.nio.http.HttpSession.sendResponse
  [18] RequestHandler2_status.handleRequest
  [19] one.nio.http.HttpServer.handleRequest
  [20] one.nio.http.HttpSession.handleParsedRequest
  [21] one.nio.http.HttpSession.processHttpBuffer
  [22] one.nio.http.HttpSession.processRead
  [23] one.nio.net.Session.process
  [24] one.nio.server.SelectorThread.run

--- 10213500 ns (0.15%), 1 sample
  [ 0] tcp_v4_send_check_[k]
  [ 1] tcp_write_xmit_[k]
  [ 2] __tcp_push_pending_frames_[k]
  [ 3] tcp_push_[k]
  [ 4] tcp_sendmsg_locked_[k]
  [ 5] tcp_sendmsg_[k]
  [ 6] inet6_sendmsg_[k]
  [ 7] sock_sendmsg_[k]
  [ 8] __sys_sendto_[k]
  [ 9] __x64_sys_sendto_[k]
  [10] do_syscall_64_[k]
  [11] entry_SYSCALL_64_after_hwframe_[k]
  [12] __libc_send
  [13] one.nio.net.NativeSocket.write
  [14] one.nio.net.Session$ArrayQueueItem.write
  [15] one.nio.net.Session.write
  [16] one.nio.net.Session.write
  [17] one.nio.http.HttpSession.writeResponse
  [18] one.nio.http.HttpSession.sendResponse
  [19] RequestHandler2_status.handleRequest
  [20] one.nio.http.HttpServer.handleRequest
  [21] one.nio.http.HttpSession.handleParsedRequest
  [22] one.nio.http.HttpSession.processHttpBuffer
  [23] one.nio.http.HttpSession.processRead
  [24] one.nio.net.Session.process
  [25] one.nio.server.SelectorThread.run

--- 10213202 ns (0.15%), 1 sample
  [ 0] tcp_small_queue_check.isra.33_[k]
  [ 1] tcp_write_xmit_[k]
  [ 2] __tcp_push_pending_frames_[k]
  [ 3] tcp_push_[k]
  [ 4] tcp_sendmsg_locked_[k]
  [ 5] tcp_sendmsg_[k]
  [ 6] inet6_sendmsg_[k]
  [ 7] sock_sendmsg_[k]
  [ 8] __sys_sendto_[k]
  [ 9] __x64_sys_sendto_[k]
  [10] do_syscall_64_[k]
  [11] entry_SYSCALL_64_after_hwframe_[k]
  [12] __libc_send
  [13] one.nio.net.NativeSocket.write
  [14] one.nio.net.Session$ArrayQueueItem.write
  [15] one.nio.net.Session.write
  [16] one.nio.net.Session.write
  [17] one.nio.http.HttpSession.writeResponse
  [18] one.nio.http.HttpSession.sendResponse
  [19] RequestHandler2_status.handleRequest
  [20] one.nio.http.HttpServer.handleRequest
  [21] one.nio.http.HttpSession.handleParsedRequest
  [22] one.nio.http.HttpSession.processHttpBuffer
  [23] one.nio.http.HttpSession.processRead
  [24] one.nio.net.Session.process
  [25] one.nio.server.SelectorThread.run

--- 10213021 ns (0.15%), 1 sample
  [ 0] __audit_syscall_entry_[k]
  [ 1] do_syscall_64_[k]
  [ 2] entry_SYSCALL_64_after_hwframe_[k]
  [ 3] __GI___recv
  [ 4] one.nio.net.NativeSocket.read
  [ 5] one.nio.net.Session.read
  [ 6] one.nio.http.HttpSession.processRead
  [ 7] one.nio.net.Session.process
  [ 8] one.nio.server.SelectorThread.run

--- 10212635 ns (0.15%), 1 sample
  [ 0] _cond_resched_[k]
  [ 1] __alloc_skb_[k]
  [ 2] sk_stream_alloc_skb_[k]
  [ 3] tcp_sendmsg_locked_[k]
  [ 4] tcp_sendmsg_[k]
  [ 5] inet6_sendmsg_[k]
  [ 6] sock_sendmsg_[k]
  [ 7] __sys_sendto_[k]
  [ 8] __x64_sys_sendto_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __libc_send
  [12] one.nio.net.NativeSocket.write
  [13] one.nio.net.Session$ArrayQueueItem.write
  [14] one.nio.net.Session.write
  [15] one.nio.net.Session.write
  [16] one.nio.http.HttpSession.writeResponse
  [17] one.nio.http.HttpSession.sendResponse
  [18] RequestHandler2_status.handleRequest
  [19] one.nio.http.HttpServer.handleRequest
  [20] one.nio.http.HttpSession.handleParsedRequest
  [21] one.nio.http.HttpSession.processHttpBuffer
  [22] one.nio.http.HttpSession.processRead
  [23] one.nio.net.Session.process
  [24] one.nio.server.SelectorThread.run

--- 10212532 ns (0.15%), 1 sample
  [ 0] read_tsc_[k]
  [ 1] tcp_write_xmit_[k]
  [ 2] __tcp_push_pending_frames_[k]
  [ 3] tcp_push_[k]
  [ 4] tcp_sendmsg_locked_[k]
  [ 5] tcp_sendmsg_[k]
  [ 6] inet6_sendmsg_[k]
  [ 7] sock_sendmsg_[k]
  [ 8] __sys_sendto_[k]
  [ 9] __x64_sys_sendto_[k]
  [10] do_syscall_64_[k]
  [11] entry_SYSCALL_64_after_hwframe_[k]
  [12] __libc_send
  [13] one.nio.net.NativeSocket.write
  [14] one.nio.net.Session$ArrayQueueItem.write
  [15] one.nio.net.Session.write
  [16] one.nio.net.Session.write
  [17] one.nio.http.HttpSession.writeResponse
  [18] one.nio.http.HttpSession.sendResponse
  [19] RequestHandler2_status.handleRequest
  [20] one.nio.http.HttpServer.handleRequest
  [21] one.nio.http.HttpSession.handleParsedRequest
  [22] one.nio.http.HttpSession.processHttpBuffer
  [23] one.nio.http.HttpSession.processRead
  [24] one.nio.net.Session.process
  [25] one.nio.server.SelectorThread.run

--- 10211883 ns (0.15%), 1 sample
  [ 0] bictcp_cwnd_event_[k]
  [ 1] tcp_write_xmit_[k]
  [ 2] __tcp_push_pending_frames_[k]
  [ 3] tcp_push_[k]
  [ 4] tcp_sendmsg_locked_[k]
  [ 5] tcp_sendmsg_[k]
  [ 6] inet6_sendmsg_[k]
  [ 7] sock_sendmsg_[k]
  [ 8] __sys_sendto_[k]
  [ 9] __x64_sys_sendto_[k]
  [10] do_syscall_64_[k]
  [11] entry_SYSCALL_64_after_hwframe_[k]
  [12] __libc_send
  [13] one.nio.net.NativeSocket.write
  [14] one.nio.net.Session$ArrayQueueItem.write
  [15] one.nio.net.Session.write
  [16] one.nio.net.Session.write
  [17] one.nio.http.HttpSession.writeResponse
  [18] one.nio.http.HttpSession.sendResponse
  [19] RequestHandler2_status.handleRequest
  [20] one.nio.http.HttpServer.handleRequest
  [21] one.nio.http.HttpSession.handleParsedRequest
  [22] one.nio.http.HttpSession.processHttpBuffer
  [23] one.nio.http.HttpSession.processRead
  [24] one.nio.net.Session.process
  [25] one.nio.server.SelectorThread.run

--- 10211702 ns (0.15%), 1 sample
  [ 0] hash_conntrack_raw?[nf_conntrack]_[k]
  [ 1] ipv4_conntrack_local?[nf_conntrack]_[k]
  [ 2] nf_hook_slow_[k]
  [ 3] __ip_local_out_[k]
  [ 4] ip_local_out_[k]
  [ 5] __ip_queue_xmit_[k]
  [ 6] ip_queue_xmit_[k]
  [ 7] __tcp_transmit_skb_[k]
  [ 8] tcp_write_xmit_[k]
  [ 9] __tcp_push_pending_frames_[k]
  [10] tcp_push_[k]
  [11] tcp_sendmsg_locked_[k]
  [12] tcp_sendmsg_[k]
  [13] inet6_sendmsg_[k]
  [14] sock_sendmsg_[k]
  [15] __sys_sendto_[k]
  [16] __x64_sys_sendto_[k]
  [17] do_syscall_64_[k]
  [18] entry_SYSCALL_64_after_hwframe_[k]
  [19] __libc_send
  [20] one.nio.net.NativeSocket.write
  [21] one.nio.net.Session$ArrayQueueItem.write
  [22] one.nio.net.Session.write
  [23] one.nio.net.Session.write
  [24] one.nio.http.HttpSession.writeResponse
  [25] one.nio.http.HttpSession.sendResponse
  [26] RequestHandler2_status.handleRequest
  [27] one.nio.http.HttpServer.handleRequest
  [28] one.nio.http.HttpSession.handleParsedRequest
  [29] one.nio.http.HttpSession.processHttpBuffer
  [30] one.nio.http.HttpSession.processRead
  [31] one.nio.net.Session.process
  [32] one.nio.server.SelectorThread.run

--- 10210784 ns (0.15%), 1 sample
  [ 0] __x64_sys_epoll_wait_[k]
  [ 1] entry_SYSCALL_64_after_hwframe_[k]
  [ 2] epoll_wait
  [ 3] [unknown]
  [ 4] one.nio.net.NativeSelector.epollWait
  [ 5] one.nio.net.NativeSelector.select
  [ 6] one.nio.server.SelectorThread.run

--- 10210747 ns (0.15%), 1 sample
  [ 0] ip_rcv_core.isra.20_[k]
  [ 1] __netif_receive_skb_one_core_[k]
  [ 2] __netif_receive_skb_[k]
  [ 3] process_backlog_[k]
  [ 4] net_rx_action_[k]
  [ 5] __softirqentry_text_start_[k]
  [ 6] do_softirq_own_stack_[k]
  [ 7] do_softirq.part.20_[k]
  [ 8] __local_bh_enable_ip_[k]
  [ 9] ip_finish_output2_[k]
  [10] __ip_finish_output_[k]
  [11] ip_finish_output_[k]
  [12] ip_output_[k]
  [13] ip_local_out_[k]
  [14] __ip_queue_xmit_[k]
  [15] ip_queue_xmit_[k]
  [16] __tcp_transmit_skb_[k]
  [17] tcp_write_xmit_[k]
  [18] __tcp_push_pending_frames_[k]
  [19] tcp_push_[k]
  [20] tcp_sendmsg_locked_[k]
  [21] tcp_sendmsg_[k]
  [22] inet6_sendmsg_[k]
  [23] sock_sendmsg_[k]
  [24] __sys_sendto_[k]
  [25] __x64_sys_sendto_[k]
  [26] do_syscall_64_[k]
  [27] entry_SYSCALL_64_after_hwframe_[k]
  [28] __libc_send
  [29] one.nio.net.NativeSocket.write
  [30] one.nio.net.Session$ArrayQueueItem.write
  [31] one.nio.net.Session.write
  [32] one.nio.net.Session.write
  [33] one.nio.http.HttpSession.writeResponse
  [34] one.nio.http.HttpSession.sendResponse
  [35] RequestHandler2_status.handleRequest
  [36] one.nio.http.HttpServer.handleRequest
  [37] one.nio.http.HttpSession.handleParsedRequest
  [38] one.nio.http.HttpSession.processHttpBuffer
  [39] one.nio.http.HttpSession.processRead
  [40] one.nio.net.Session.process
  [41] one.nio.server.SelectorThread.run

--- 10210041 ns (0.15%), 1 sample
  [ 0] __usecs_to_jiffies_[k]
  [ 1] net_rx_action_[k]
  [ 2] __softirqentry_text_start_[k]
  [ 3] do_softirq_own_stack_[k]
  [ 4] do_softirq.part.20_[k]
  [ 5] __local_bh_enable_ip_[k]
  [ 6] ip_finish_output2_[k]
  [ 7] __ip_finish_output_[k]
  [ 8] ip_finish_output_[k]
  [ 9] ip_output_[k]
  [10] ip_local_out_[k]
  [11] __ip_queue_xmit_[k]
  [12] ip_queue_xmit_[k]
  [13] __tcp_transmit_skb_[k]
  [14] tcp_write_xmit_[k]
  [15] __tcp_push_pending_frames_[k]
  [16] tcp_push_[k]
  [17] tcp_sendmsg_locked_[k]
  [18] tcp_sendmsg_[k]
  [19] inet6_sendmsg_[k]
  [20] sock_sendmsg_[k]
  [21] __sys_sendto_[k]
  [22] __x64_sys_sendto_[k]
  [23] do_syscall_64_[k]
  [24] entry_SYSCALL_64_after_hwframe_[k]
  [25] __libc_send
  [26] one.nio.net.NativeSocket.write
  [27] one.nio.net.Session$ArrayQueueItem.write
  [28] one.nio.net.Session.write
  [29] one.nio.net.Session.write
  [30] one.nio.http.HttpSession.writeResponse
  [31] one.nio.http.HttpSession.sendResponse
  [32] RequestHandler2_status.handleRequest
  [33] one.nio.http.HttpServer.handleRequest
  [34] one.nio.http.HttpSession.handleParsedRequest
  [35] one.nio.http.HttpSession.processHttpBuffer
  [36] one.nio.http.HttpSession.processRead
  [37] one.nio.net.Session.process
  [38] one.nio.server.SelectorThread.run

--- 10209866 ns (0.15%), 1 sample
  [ 0] _raw_spin_lock_irqsave_[k]
  [ 1] __wake_up_common_lock_[k]
  [ 2] __wake_up_sync_key_[k]
  [ 3] sock_def_readable_[k]
  [ 4] tcp_data_ready_[k]
  [ 5] tcp_rcv_established_[k]
  [ 6] tcp_v4_do_rcv_[k]
  [ 7] tcp_v4_rcv_[k]
  [ 8] ip_protocol_deliver_rcu_[k]
  [ 9] ip_local_deliver_finish_[k]
  [10] ip_local_deliver_[k]
  [11] ip_rcv_finish_[k]
  [12] ip_rcv_[k]
  [13] __netif_receive_skb_one_core_[k]
  [14] __netif_receive_skb_[k]
  [15] process_backlog_[k]
  [16] net_rx_action_[k]
  [17] __softirqentry_text_start_[k]
  [18] do_softirq_own_stack_[k]
  [19] do_softirq.part.20_[k]
  [20] __local_bh_enable_ip_[k]
  [21] ip_finish_output2_[k]
  [22] __ip_finish_output_[k]
  [23] ip_finish_output_[k]
  [24] ip_output_[k]
  [25] ip_local_out_[k]
  [26] __ip_queue_xmit_[k]
  [27] ip_queue_xmit_[k]
  [28] __tcp_transmit_skb_[k]
  [29] tcp_write_xmit_[k]
  [30] __tcp_push_pending_frames_[k]
  [31] tcp_push_[k]
  [32] tcp_sendmsg_locked_[k]
  [33] tcp_sendmsg_[k]
  [34] inet6_sendmsg_[k]
  [35] sock_sendmsg_[k]
  [36] __sys_sendto_[k]
  [37] __x64_sys_sendto_[k]
  [38] do_syscall_64_[k]
  [39] entry_SYSCALL_64_after_hwframe_[k]
  [40] __libc_send
  [41] one.nio.net.NativeSocket.write
  [42] one.nio.net.Session$ArrayQueueItem.write
  [43] one.nio.net.Session.write
  [44] one.nio.net.Session.write
  [45] one.nio.http.HttpSession.writeResponse
  [46] one.nio.http.HttpSession.sendResponse
  [47] RequestHandler2_status.handleRequest
  [48] one.nio.http.HttpServer.handleRequest
  [49] one.nio.http.HttpSession.handleParsedRequest
  [50] one.nio.http.HttpSession.processHttpBuffer
  [51] one.nio.http.HttpSession.processRead
  [52] one.nio.net.Session.process
  [53] one.nio.server.SelectorThread.run

--- 10209771 ns (0.15%), 1 sample
  [ 0] schedule_[k]
  [ 1] schedule_hrtimeout_range_[k]
  [ 2] ep_poll_[k]
  [ 3] do_epoll_wait_[k]
  [ 4] __x64_sys_epoll_wait_[k]
  [ 5] do_syscall_64_[k]
  [ 6] entry_SYSCALL_64_after_hwframe_[k]
  [ 7] epoll_wait
  [ 8] [unknown]
  [ 9] one.nio.net.NativeSelector.epollWait
  [10] one.nio.net.NativeSelector.select
  [11] one.nio.server.SelectorThread.run

--- 10209643 ns (0.15%), 1 sample
  [ 0] syscall_slow_exit_work_[k]
  [ 1] do_syscall_64_[k]
  [ 2] entry_SYSCALL_64_after_hwframe_[k]
  [ 3] epoll_wait
  [ 4] [unknown]
  [ 5] one.nio.net.NativeSelector.epollWait
  [ 6] one.nio.net.NativeSelector.select
  [ 7] one.nio.server.SelectorThread.run

--- 10208742 ns (0.15%), 1 sample
  [ 0] __x86_indirect_thunk_rax_[k]
  [ 1] skb_release_all_[k]
  [ 2] __kfree_skb_[k]
  [ 3] tcp_recvmsg_[k]
  [ 4] inet6_recvmsg_[k]
  [ 5] sock_recvmsg_[k]
  [ 6] __sys_recvfrom_[k]
  [ 7] __x64_sys_recvfrom_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __GI___recv
  [11] one.nio.net.NativeSocket.read
  [12] one.nio.net.Session.read
  [13] one.nio.http.HttpSession.processRead
  [14] one.nio.net.Session.process
  [15] one.nio.server.SelectorThread.run

--- 10208188 ns (0.15%), 1 sample
  [ 0] apparmor_socket_sendmsg_[k]
  [ 1] sock_sendmsg_[k]
  [ 2] __sys_sendto_[k]
  [ 3] __x64_sys_sendto_[k]
  [ 4] do_syscall_64_[k]
  [ 5] entry_SYSCALL_64_after_hwframe_[k]
  [ 6] __libc_send
  [ 7] one.nio.net.NativeSocket.write
  [ 8] one.nio.net.Session$ArrayQueueItem.write
  [ 9] one.nio.net.Session.write
  [10] one.nio.net.Session.write
  [11] one.nio.http.HttpSession.writeResponse
  [12] one.nio.http.HttpSession.sendResponse
  [13] RequestHandler2_status.handleRequest
  [14] one.nio.http.HttpServer.handleRequest
  [15] one.nio.http.HttpSession.handleParsedRequest
  [16] one.nio.http.HttpSession.processHttpBuffer
  [17] one.nio.http.HttpSession.processRead
  [18] one.nio.net.Session.process
  [19] one.nio.server.SelectorThread.run

--- 10208028 ns (0.15%), 1 sample
  [ 0] memmove@plt
  [ 1] Java_one_nio_net_NativeSocket_read
  [ 2] one.nio.net.NativeSocket.read
  [ 3] one.nio.net.Session.read
  [ 4] one.nio.http.HttpSession.processRead
  [ 5] one.nio.net.Session.process
  [ 6] one.nio.server.SelectorThread.run

--- 10207368 ns (0.15%), 1 sample
  [ 0] bictcp_acked_[k]
  [ 1] tcp_clean_rtx_queue_[k]
  [ 2] tcp_ack_[k]
  [ 3] tcp_rcv_established_[k]
  [ 4] tcp_v4_do_rcv_[k]
  [ 5] tcp_v4_rcv_[k]
  [ 6] ip_protocol_deliver_rcu_[k]
  [ 7] ip_local_deliver_finish_[k]
  [ 8] ip_local_deliver_[k]
  [ 9] ip_rcv_finish_[k]
  [10] ip_rcv_[k]
  [11] __netif_receive_skb_one_core_[k]
  [12] __netif_receive_skb_[k]
  [13] process_backlog_[k]
  [14] net_rx_action_[k]
  [15] __softirqentry_text_start_[k]
  [16] do_softirq_own_stack_[k]
  [17] do_softirq.part.20_[k]
  [18] __local_bh_enable_ip_[k]
  [19] ip_finish_output2_[k]
  [20] __ip_finish_output_[k]
  [21] ip_finish_output_[k]
  [22] ip_output_[k]
  [23] ip_local_out_[k]
  [24] __ip_queue_xmit_[k]
  [25] ip_queue_xmit_[k]
  [26] __tcp_transmit_skb_[k]
  [27] tcp_write_xmit_[k]
  [28] __tcp_push_pending_frames_[k]
  [29] tcp_push_[k]
  [30] tcp_sendmsg_locked_[k]
  [31] tcp_sendmsg_[k]
  [32] inet6_sendmsg_[k]
  [33] sock_sendmsg_[k]
  [34] __sys_sendto_[k]
  [35] __x64_sys_sendto_[k]
  [36] do_syscall_64_[k]
  [37] entry_SYSCALL_64_after_hwframe_[k]
  [38] __libc_send
  [39] one.nio.net.NativeSocket.write
  [40] one.nio.net.Session$ArrayQueueItem.write
  [41] one.nio.net.Session.write
  [42] one.nio.net.Session.write
  [43] one.nio.http.HttpSession.writeResponse
  [44] one.nio.http.HttpSession.sendResponse
  [45] RequestHandler2_status.handleRequest
  [46] one.nio.http.HttpServer.handleRequest
  [47] one.nio.http.HttpSession.handleParsedRequest
  [48] one.nio.http.HttpSession.processHttpBuffer
  [49] one.nio.http.HttpSession.processRead
  [50] one.nio.net.Session.process
  [51] one.nio.server.SelectorThread.run

--- 10206530 ns (0.15%), 1 sample
  [ 0] lock_sock_nested_[k]
  [ 1] inet6_recvmsg_[k]
  [ 2] sock_recvmsg_[k]
  [ 3] __sys_recvfrom_[k]
  [ 4] __x64_sys_recvfrom_[k]
  [ 5] do_syscall_64_[k]
  [ 6] entry_SYSCALL_64_after_hwframe_[k]
  [ 7] __GI___recv
  [ 8] one.nio.net.NativeSocket.read
  [ 9] one.nio.net.Session.read
  [10] one.nio.http.HttpSession.processRead
  [11] one.nio.net.Session.process
  [12] one.nio.server.SelectorThread.run

--- 10206270 ns (0.15%), 1 sample
  [ 0] __kmalloc_reserve.isra.62_[k]
  [ 1] __alloc_skb_[k]
  [ 2] sk_stream_alloc_skb_[k]
  [ 3] tcp_sendmsg_locked_[k]
  [ 4] tcp_sendmsg_[k]
  [ 5] inet6_sendmsg_[k]
  [ 6] sock_sendmsg_[k]
  [ 7] __sys_sendto_[k]
  [ 8] __x64_sys_sendto_[k]
  [ 9] do_syscall_64_[k]
  [10] entry_SYSCALL_64_after_hwframe_[k]
  [11] __libc_send
  [12] one.nio.net.NativeSocket.write
  [13] one.nio.net.Session$ArrayQueueItem.write
  [14] one.nio.net.Session.write
  [15] one.nio.net.Session.write
  [16] one.nio.http.HttpSession.writeResponse
  [17] one.nio.http.HttpSession.sendResponse
  [18] RequestHandler2_status.handleRequest
  [19] one.nio.http.HttpServer.handleRequest
  [20] one.nio.http.HttpSession.handleParsedRequest
  [21] one.nio.http.HttpSession.processHttpBuffer
  [22] one.nio.http.HttpSession.processRead
  [23] one.nio.net.Session.process
  [24] one.nio.server.SelectorThread.run

--- 10205574 ns (0.15%), 1 sample
  [ 0] ThreadInVMfromNative::~ThreadInVMfromNative()
  [ 1] jni_GetByteArrayRegion
  [ 2] Java_one_nio_net_NativeSocket_write
  [ 3] one.nio.net.NativeSocket.write
  [ 4] one.nio.net.Session$ArrayQueueItem.write
  [ 5] one.nio.net.Session.write
  [ 6] one.nio.net.Session.write
  [ 7] one.nio.http.HttpSession.writeResponse
  [ 8] one.nio.http.HttpSession.sendResponse
  [ 9] RequestHandler2_status.handleRequest
  [10] one.nio.http.HttpServer.handleRequest
  [11] one.nio.http.HttpSession.handleParsedRequest
  [12] one.nio.http.HttpSession.processHttpBuffer
  [13] one.nio.http.HttpSession.processRead
  [14] one.nio.net.Session.process
  [15] one.nio.server.SelectorThread.run

--- 10205421 ns (0.15%), 1 sample
  [ 0] ipv4_confirm?[nf_conntrack]_[k]
  [ 1] nf_hook_slow_[k]
  [ 2] ip_output_[k]
  [ 3] ip_local_out_[k]
  [ 4] __ip_queue_xmit_[k]
  [ 5] ip_queue_xmit_[k]
  [ 6] __tcp_transmit_skb_[k]
  [ 7] tcp_write_xmit_[k]
  [ 8] __tcp_push_pending_frames_[k]
  [ 9] tcp_push_[k]
  [10] tcp_sendmsg_locked_[k]
  [11] tcp_sendmsg_[k]
  [12] inet6_sendmsg_[k]
  [13] sock_sendmsg_[k]
  [14] __sys_sendto_[k]
  [15] __x64_sys_sendto_[k]
  [16] do_syscall_64_[k]
  [17] entry_SYSCALL_64_after_hwframe_[k]
  [18] __libc_send
  [19] one.nio.net.NativeSocket.write
  [20] one.nio.net.Session$ArrayQueueItem.write
  [21] one.nio.net.Session.write
  [22] one.nio.net.Session.write
  [23] one.nio.http.HttpSession.writeResponse
  [24] one.nio.http.HttpSession.sendResponse
  [25] RequestHandler2_status.handleRequest
  [26] one.nio.http.HttpServer.handleRequest
  [27] one.nio.http.HttpSession.handleParsedRequest
  [28] one.nio.http.HttpSession.processHttpBuffer
  [29] one.nio.http.HttpSession.processRead
  [30] one.nio.net.Session.process
  [31] one.nio.server.SelectorThread.run

--- 10204525 ns (0.15%), 1 sample
  [ 0] __memmove_avx_unaligned_erms
  [ 1] Java_one_nio_net_NativeSocket_write
  [ 2] one.nio.net.NativeSocket.write
  [ 3] one.nio.net.Session$ArrayQueueItem.write
  [ 4] one.nio.net.Session.write
  [ 5] one.nio.net.Session.write
  [ 6] one.nio.http.HttpSession.writeResponse
  [ 7] one.nio.http.HttpSession.sendResponse
  [ 8] RequestHandler2_status.handleRequest
  [ 9] one.nio.http.HttpServer.handleRequest
  [10] one.nio.http.HttpSession.handleParsedRequest
  [11] one.nio.http.HttpSession.processHttpBuffer
  [12] one.nio.http.HttpSession.processRead
  [13] one.nio.net.Session.process
  [14] one.nio.server.SelectorThread.run

--- 10204316 ns (0.15%), 1 sample
  [ 0] _raw_spin_lock_bh_[k]
  [ 1] release_sock_[k]
  [ 2] tcp_recvmsg_[k]
  [ 3] inet6_recvmsg_[k]
  [ 4] sock_recvmsg_[k]
  [ 5] __sys_recvfrom_[k]
  [ 6] __x64_sys_recvfrom_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] __GI___recv
  [10] one.nio.net.NativeSocket.read
  [11] one.nio.net.Session.read
  [12] one.nio.http.HttpSession.processRead
  [13] one.nio.net.Session.process
  [14] one.nio.server.SelectorThread.run

--- 10204210 ns (0.15%), 1 sample
  [ 0] __skb_clone_[k]
  [ 1] __tcp_transmit_skb_[k]
  [ 2] tcp_write_xmit_[k]
  [ 3] __tcp_push_pending_frames_[k]
  [ 4] tcp_push_[k]
  [ 5] tcp_sendmsg_locked_[k]
  [ 6] tcp_sendmsg_[k]
  [ 7] inet6_sendmsg_[k]
  [ 8] sock_sendmsg_[k]
  [ 9] __sys_sendto_[k]
  [10] __x64_sys_sendto_[k]
  [11] do_syscall_64_[k]
  [12] entry_SYSCALL_64_after_hwframe_[k]
  [13] __libc_send
  [14] one.nio.net.NativeSocket.write
  [15] one.nio.net.Session$ArrayQueueItem.write
  [16] one.nio.net.Session.write
  [17] one.nio.net.Session.write
  [18] one.nio.http.HttpSession.writeResponse
  [19] one.nio.http.HttpSession.sendResponse
  [20] RequestHandler2_status.handleRequest
  [21] one.nio.http.HttpServer.handleRequest
  [22] one.nio.http.HttpSession.handleParsedRequest
  [23] one.nio.http.HttpSession.processHttpBuffer
  [24] one.nio.http.HttpSession.processRead
  [25] one.nio.net.Session.process
  [26] one.nio.server.SelectorThread.run

--- 10203883 ns (0.15%), 1 sample
  [ 0] simple_copy_to_iter_[k]
  [ 1] __skb_datagram_iter_[k]
  [ 2] skb_copy_datagram_iter_[k]
  [ 3] tcp_recvmsg_[k]
  [ 4] inet6_recvmsg_[k]
  [ 5] sock_recvmsg_[k]
  [ 6] __sys_recvfrom_[k]
  [ 7] __x64_sys_recvfrom_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __GI___recv
  [11] one.nio.net.NativeSocket.read
  [12] one.nio.net.Session.read
  [13] one.nio.http.HttpSession.processRead
  [14] one.nio.net.Session.process
  [15] one.nio.server.SelectorThread.run

--- 10203856 ns (0.15%), 1 sample
  [ 0] syscall_trace_enter_[k]
  [ 1] entry_SYSCALL_64_after_hwframe_[k]
  [ 2] __GI___recv
  [ 3] one.nio.net.NativeSocket.read
  [ 4] one.nio.net.Session.read
  [ 5] one.nio.http.HttpSession.processRead
  [ 6] one.nio.net.Session.process
  [ 7] one.nio.server.SelectorThread.run

--- 10203854 ns (0.15%), 1 sample
  [ 0] iov_iter_advance_[k]
  [ 1] _copy_from_iter_full_[k]
  [ 2] tcp_sendmsg_locked_[k]
  [ 3] tcp_sendmsg_[k]
  [ 4] inet6_sendmsg_[k]
  [ 5] sock_sendmsg_[k]
  [ 6] __sys_sendto_[k]
  [ 7] __x64_sys_sendto_[k]
  [ 8] do_syscall_64_[k]
  [ 9] entry_SYSCALL_64_after_hwframe_[k]
  [10] __libc_send
  [11] one.nio.net.NativeSocket.write
  [12] one.nio.net.Session$ArrayQueueItem.write
  [13] one.nio.net.Session.write
  [14] one.nio.net.Session.write
  [15] one.nio.http.HttpSession.writeResponse
  [16] one.nio.http.HttpSession.sendResponse
  [17] RequestHandler2_status.handleRequest
  [18] one.nio.http.HttpServer.handleRequest
  [19] one.nio.http.HttpSession.handleParsedRequest
  [20] one.nio.http.HttpSession.processHttpBuffer
  [21] one.nio.http.HttpSession.processRead
  [22] one.nio.net.Session.process
  [23] one.nio.server.SelectorThread.run

--- 10203829 ns (0.15%), 1 sample
  [ 0] __skb_datagram_iter_[k]
  [ 1] skb_copy_datagram_iter_[k]
  [ 2] tcp_recvmsg_[k]
  [ 3] inet6_recvmsg_[k]
  [ 4] sock_recvmsg_[k]
  [ 5] __sys_recvfrom_[k]
  [ 6] __x64_sys_recvfrom_[k]
  [ 7] do_syscall_64_[k]
  [ 8] entry_SYSCALL_64_after_hwframe_[k]
  [ 9] __GI___recv
  [10] one.nio.net.NativeSocket.read
  [11] one.nio.net.Session.read
  [12] one.nio.http.HttpSession.processRead
  [13] one.nio.net.Session.process
  [14] one.nio.server.SelectorThread.run

--- 10203672 ns (0.15%), 1 sample
  [ 0] netif_rx_[k]
  [ 1] dev_hard_start_xmit_[k]
  [ 2] __dev_queue_xmit_[k]
  [ 3] dev_queue_xmit_[k]
  [ 4] ip_finish_output2_[k]
  [ 5] __ip_finish_output_[k]
  [ 6] ip_finish_output_[k]
  [ 7] ip_output_[k]
  [ 8] ip_local_out_[k]
  [ 9] __ip_queue_xmit_[k]
  [10] ip_queue_xmit_[k]
  [11] __tcp_transmit_skb_[k]
  [12] tcp_write_xmit_[k]
  [13] __tcp_push_pending_frames_[k]
  [14] tcp_push_[k]
  [15] tcp_sendmsg_locked_[k]
  [16] tcp_sendmsg_[k]
  [17] inet6_sendmsg_[k]
  [18] sock_sendmsg_[k]
  [19] __sys_sendto_[k]
  [20] __x64_sys_sendto_[k]
  [21] do_syscall_64_[k]
  [22] entry_SYSCALL_64_after_hwframe_[k]
  [23] __libc_send
  [24] one.nio.net.NativeSocket.write
  [25] one.nio.net.Session$ArrayQueueItem.write
  [26] one.nio.net.Session.write
  [27] one.nio.net.Session.write
  [28] one.nio.http.HttpSession.writeResponse
  [29] one.nio.http.HttpSession.sendResponse
  [30] RequestHandler2_status.handleRequest
  [31] one.nio.http.HttpServer.handleRequest
  [32] one.nio.http.HttpSession.handleParsedRequest
  [33] one.nio.http.HttpSession.processHttpBuffer
  [34] one.nio.http.HttpSession.processRead
  [35] one.nio.net.Session.process
  [36] one.nio.server.SelectorThread.run

--- 10203319 ns (0.15%), 1 sample
  [ 0] ktime_get_[k]
  [ 1] tcp_write_xmit_[k]
  [ 2] __tcp_push_pending_frames_[k]
  [ 3] tcp_push_[k]
  [ 4] tcp_sendmsg_locked_[k]
  [ 5] tcp_sendmsg_[k]
  [ 6] inet6_sendmsg_[k]
  [ 7] sock_sendmsg_[k]
  [ 8] __sys_sendto_[k]
  [ 9] __x64_sys_sendto_[k]
  [10] do_syscall_64_[k]
  [11] entry_SYSCALL_64_after_hwframe_[k]
  [12] __libc_send
  [13] one.nio.net.NativeSocket.write
  [14] one.nio.net.Session$ArrayQueueItem.write
  [15] one.nio.net.Session.write
  [16] one.nio.net.Session.write
  [17] one.nio.http.HttpSession.writeResponse
  [18] one.nio.http.HttpSession.sendResponse
  [19] RequestHandler2_status.handleRequest
  [20] one.nio.http.HttpServer.handleRequest
  [21] one.nio.http.HttpSession.handleParsedRequest
  [22] one.nio.http.HttpSession.processHttpBuffer
  [23] one.nio.http.HttpSession.processRead
  [24] one.nio.net.Session.process
  [25] one.nio.server.SelectorThread.run

          ns  percent  samples  top
  ----------  -------  -------  ---
   142511144    2.07%       14  java.lang.StringLatin1.indexOf
   122353866    1.78%       12  one.nio.net.NativeSelector.epollWait
   122285574    1.78%       12  [vdso]
   122209820    1.77%       12  ipt_do_table?[ip_tables]_[k]
   122208116    1.77%       12  one.nio.util.Utf8.startsWith
   111976334    1.63%       11  __lock_text_start_[k]
   102205352    1.48%       10  do_syscall_64_[k]
   101940305    1.48%       10  java.lang.StringUTF16.checkIndex
   101887035    1.48%       10  __tcp_transmit_skb_[k]
   101837817    1.48%       10  one.nio.http.Response.toBytes
    91738448    1.33%        9  aa_sk_perm_[k]
    91653576    1.33%        9  clock_gettime
    91626865    1.33%        9  __ksize_[k]
    81584450    1.18%        8  epoll_wait
    81579409    1.18%        8  __fget_[k]
    81556485    1.18%        8  tcp_ack_[k]
    81515724    1.18%        8  tcp_sendmsg_locked_[k]
    81494617    1.18%        8  __check_object_size_[k]
    81467530    1.18%        8  __inet_lookup_established_[k]
    81436539    1.18%        8  one.nio.http.Request.getHeader
    71383571    1.04%        7  __nf_conntrack_find_get?[nf_conntrack]_[k]
    71358705    1.04%        7  tcp_recvmsg_[k]
    71352372    1.04%        7  jni_SetByteArrayRegion
    71316828    1.04%        7  __kmalloc_node_track_caller_[k]
    71279815    1.03%        7  java.util.HashMap.getNode
    71256852    1.03%        7  __slab_free_[k]
    71238445    1.03%        7  aa_label_sk_perm.part.4_[k]
    61186180    0.89%        6  ep_scan_ready_list.constprop.20_[k]
    61184944    0.89%        6  eth_type_trans_[k]
    61164647    0.89%        6  nf_conntrack_in?[nf_conntrack]_[k]
    61163508    0.89%        6  syscall_trace_enter_[k]
    61120315    0.89%        6  net_rx_action_[k]
    61081057    0.89%        6  __ip_queue_xmit_[k]
    51006066    0.74%        5  one.nio.net.NativeSelector.select
    50999001    0.74%        5  HandleMark::pop_and_restore()
    50980892    0.74%        5  one.nio.server.SelectorThread.run
    50962924    0.74%        5  tcp_in_window?[nf_conntrack]_[k]
    50959757    0.74%        5  skb_release_data_[k]
    50949899    0.74%        5  __fget_light_[k]
    50949566    0.74%        5  __libc_disable_asynccancel
    50949547    0.74%        5  read_tsc_[k]
    50929323    0.74%        5  nf_ct_get_tuple?[nf_conntrack]_[k]
    50919379    0.74%        5  one.nio.net.NativeSocket.read
    50887434    0.74%        5  _raw_spin_lock_bh_[k]
    50860150    0.74%        5  __libc_send
    50034633    0.73%        5  SpinPause
    41573095    0.60%        4  tcp_v4_rcv_[k]
    40928437    0.59%        4  __ip_finish_output_[k]
    40803502    0.59%        4  ThreadInVMfromNative::~ThreadInVMfromNative()
    40800894    0.59%        4  get_l4proto?[nf_conntrack]_[k]
    40795823    0.59%        4  copy_user_generic_unrolled_[k]
    40795590    0.59%        4  loopback_xmit_[k]
    40792276    0.59%        4  ipv4_dst_check_[k]
    40779629    0.59%        4  sock_def_readable_[k]
    40773433    0.59%        4  __netif_receive_skb_core_[k]
    40766544    0.59%        4  tcp_clean_rtx_queue_[k]
    40766481    0.59%        4  aa_profile_af_perm_[k]
    40764945    0.59%        4  tcp_wfree_[k]
    40753982    0.59%        4  one.nio.http.HttpSession.processHttpBuffer
    40752841    0.59%        4  tcp_current_mss_[k]
    40748291    0.59%        4  clock_gettime
    40740321    0.59%        4  __dev_queue_xmit_[k]
    40736990    0.59%        4  import_single_range_[k]
    40730708    0.59%        4  ip_finish_output2_[k]
    40723472    0.59%        4  fput_many_[k]
    40701501    0.59%        4  _raw_spin_unlock_bh_[k]
    40695212    0.59%        4  ipv4_conntrack_defrag?[nf_defrag_ipv4]_[k]
    30654064    0.44%        3  __GI___recv
    30620298    0.44%        3  one.nio.net.Session.read
    30613003    0.44%        3  security_socket_recvmsg_[k]
    30612239    0.44%        3  ip_rcv_finish_core.isra.18_[k]
    30605147    0.44%        3  __skb_clone_[k]
    30604472    0.44%        3  tcp_schedule_loss_probe_[k]
    30594485    0.44%        3  __tcp_select_window_[k]
    30593766    0.44%        3  tcp_rcv_established_[k]
    30591398    0.44%        3  __sk_dst_check_[k]
    30589203    0.44%        3  tcp_v4_fill_cb_[k]
    30584609    0.44%        3  ktime_get_[k]
    30576982    0.44%        3  mod_timer_[k]
    30575887    0.44%        3  ep_poll_[k]
    30574450    0.44%        3  unroll_tree_refs_[k]
    30565737    0.44%        3  process_backlog_[k]
    30563171    0.44%        3  tcp_event_new_data_sent_[k]
    30562293    0.44%        3  ip_output_[k]
    30559668    0.44%        3  memset_erms_[k]
    30558340    0.44%        3  __audit_syscall_exit_[k]
    30556824    0.44%        3  syscall_slow_exit_work_[k]
    30555057    0.44%        3  enqueue_to_backlog_[k]
    30547429    0.44%        3  java.lang.StringLatin1.regionMatchesCI
    30539972    0.44%        3  skb_page_frag_refill_[k]
    30536377    0.44%        3  _raw_spin_lock_[k]
    30525453    0.44%        3  __libc_enable_asynccancel
    30516587    0.44%        3  do_softirq.part.20_[k]
    20434610    0.30%        2  ipv4_mtu_[k]
    20420069    0.30%        2  __slab_alloc_[k]
    20417544    0.30%        2  check_bounds(int, int, int, Thread*)
    20415524    0.30%        2  rb_insert_color_[k]
    20414995    0.30%        2  ip_rcv_[k]
    20412614    0.30%        2  sock_sendmsg_[k]
    20411900    0.30%        2  Java_one_nio_net_NativeSelector_epollWait
    20411770    0.30%        2  kmem_cache_free_[k]
    20410117    0.30%        2  ipv4_conntrack_local?[nf_conntrack]_[k]
    20404719    0.30%        2  tcp_event_data_recv_[k]
    20403812    0.30%        2  kmem_cache_alloc_node_[k]
    20401959    0.30%        2  inet6_sendmsg_[k]
    20400201    0.30%        2  tcp_send_delayed_ack_[k]
    20398194    0.30%        2  validate_xmit_xfrm_[k]
    20397834    0.30%        2  sock_recvmsg_[k]
    20396476    0.30%        2  hash_conntrack_raw?[nf_conntrack]_[k]
    20395220    0.30%        2  one.nio.net.NativeSocket.write
    20395059    0.30%        2  ip_rcv_core.isra.20_[k]
    20394022    0.30%        2  schedule_[k]
    20387809    0.30%        2  validate_xmit_skb_[k]
    20387665    0.30%        2  __local_bh_enable_ip_[k]
    20387658    0.30%        2  jbyte_disjoint_arraycopy
    20385933    0.30%        2  __cgroup_bpf_run_filter_skb_[k]
    20379600    0.30%        2  __softirqentry_text_start_[k]
    20377191    0.30%        2  release_sock_[k]
    20374468    0.30%        2  one.nio.net.Session.write
    20373470    0.30%        2  netif_skb_features_[k]
    20368912    0.30%        2  sock_poll_[k]
    20368415    0.30%        2  tcp_release_cb_[k]
    20368175    0.30%        2  Java_one_nio_net_NativeSocket_read
    20365673    0.30%        2  iptable_filter_hook?[iptable_filter]_[k]
    20364816    0.30%        2  tcp_write_xmit_[k]
    20364713    0.30%        2  kfree_[k]
    20364507    0.30%        2  one.nio.http.HttpServer.handleRequest
    20364449    0.30%        2  nf_hook_slow_[k]
    20363713    0.30%        2  __kfree_skb_flush_[k]
    20362941    0.30%        2  security_sock_rcv_skb_[k]
    20356583    0.30%        2  tcp_ack_update_rtt.isra.45_[k]
    20355159    0.30%        2  tcp_queue_rcv_[k]
    20353538    0.30%        2  __alloc_skb_[k]
    20353176    0.30%        2  apparmor_socket_recvmsg_[k]
    20352895    0.30%        2  dst_release_[k]
    20352881    0.30%        2  tcp_cleanup_rbuf_[k]
    20351884    0.30%        2  nf_ct_deliver_cached_events?[nf_conntrack]_[k]
    20348595    0.30%        2  ktime_get_seconds_[k]
    20341916    0.30%        2  Java_one_nio_net_NativeSocket_write
    20323079    0.30%        2  __x64_sys_recvfrom_[k]
    20021331    0.29%        2  HeapRegion::block_size(HeapWord const*) const
    10441599    0.15%        1  schedule_hrtimeout_range_clock_[k]
    10234327    0.15%        1  ep_send_events_proc_[k]
    10226763    0.15%        1  __kfree_skb_[k]
    10222025    0.15%        1  __sched_text_start_[k]
    10220769    0.15%        1  tcp_push_[k]
    10219672    0.15%        1  tcp_rate_skb_delivered_[k]
    10218681    0.15%        1  one.nio.http.HttpSession.writeResponse
    10218053    0.15%        1  __virt_addr_valid_[k]
    10216889    0.15%        1  kfree_skbmem_[k]
    10216297    0.15%        1  java.lang.StringLatin1.hashCode
    10216051    0.15%        1  _raw_write_lock_irq_[k]
    10215640    0.15%        1  skb_network_protocol_[k]
    10215628    0.15%        1  __netif_receive_skb_one_core_[k]
    10213500    0.15%        1  tcp_v4_send_check_[k]
    10213202    0.15%        1  tcp_small_queue_check.isra.33_[k]
    10213021    0.15%        1  __audit_syscall_entry_[k]
    10212635    0.15%        1  _cond_resched_[k]
    10211883    0.15%        1  bictcp_cwnd_event_[k]
    10210784    0.15%        1  __x64_sys_epoll_wait_[k]
    10210041    0.15%        1  __usecs_to_jiffies_[k]
    10209866    0.15%        1  _raw_spin_lock_irqsave_[k]
    10208742    0.15%        1  __x86_indirect_thunk_rax_[k]
    10208188    0.15%        1  apparmor_socket_sendmsg_[k]
    10208028    0.15%        1  memmove@plt
    10207368    0.15%        1  bictcp_acked_[k]
    10206530    0.15%        1  lock_sock_nested_[k]
    10206270    0.15%        1  __kmalloc_reserve.isra.62_[k]
    10205421    0.15%        1  ipv4_confirm?[nf_conntrack]_[k]
    10204525    0.15%        1  __memmove_avx_unaligned_erms
    10203883    0.15%        1  simple_copy_to_iter_[k]
    10203854    0.15%        1  iov_iter_advance_[k]
    10203829    0.15%        1  __skb_datagram_iter_[k]
    10203672    0.15%        1  netif_rx_[k]
    10201814    0.15%        1  raw_local_deliver_[k]
    10201502    0.15%        1  recv@plt
    10200394    0.15%        1  sk_filter_trim_cap_[k]
    10199154    0.15%        1  tcp_rcv_space_adjust_[k]
    10198810    0.15%        1  ResourceMark::reset_to_mark()
    10197421    0.15%        1  __x64_sys_sendto_[k]
    10197265    0.15%        1  __sys_sendto_[k]
    10197107    0.15%        1  CodeHeap::find_blob_unsafe(void*) const
    10194829    0.15%        1  __ip_local_out_[k]
    10194117    0.15%        1  MemAllocator::Allocation::notify_allocation_jvmti_sampler()
    10193314    0.15%        1  clear_page_erms_[k]
    10191477    0.15%        1  skb_copy_datagram_iter_[k]
    10191104    0.15%        1  one.nio.net.Session.process
    10190139    0.15%        1  ip_local_deliver_[k]
    10190076    0.15%        1  rb_next_[k]
    10188778    0.15%        1  bictcp_cong_avoid_[k]
    10188598    0.15%        1  ip_copy_addrs_[k]
    10188363    0.15%        1  __tcp_ack_snd_check_[k]
    10187987    0.15%        1  rcu_all_qs_[k]
    10186103    0.15%        1  skb_clone_tx_timestamp_[k]
    10185584    0.15%        1  tcp_update_skb_after_send_[k]
    10185470    0.15%        1  ThreadStateTransition::transition_from_native(JavaThread*, JavaThreadState) [clone .constprop.222]
    10184219    0.15%        1  nf_conntrack_tcp_packet?[nf_conntrack]_[k]
    10182211    0.15%        1  epoll_wait@plt
    10180951    0.15%        1  tcp_rack_advance_[k]
    10179534    0.15%        1  aa_apply_modes_to_perms_[k]
