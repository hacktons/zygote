package cn.hacktons.restartdemo

import cn.hacktons.zygote.App.Zygote
// 自定义的中间重启页面，在重启前加一个loading等待
class CustomRestartActivity : Zygote() {

    override fun setContent() {
        super.setContent()
        setContentView(R.layout.activity_custom_restart)
    }
}