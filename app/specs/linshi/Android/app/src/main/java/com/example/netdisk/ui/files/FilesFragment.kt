package com.example.netdisk.ui.files

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.netdisk.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FilesFragment : Fragment() {

    private val mockFiles = listOf(
        FileItem("1", "工作文档", FileType.FOLDER, updatedAt = "2026-04-20"),
        FileItem("2", "个人照片", FileType.FOLDER, updatedAt = "2026-04-19"),
        FileItem("3", "项目报告.pdf", FileType.FILE, "2.5 MB", "pdf", "2026-04-21"),
        FileItem("4", "设计稿.png", FileType.FILE, "1.8 MB", "png", "2026-04-20"),
        FileItem("5", "演示视频.mp4", FileType.FILE, "45.2 MB", "mp4", "2026-04-18"),
        FileItem("6", "会议记录.docx", FileType.FILE, "156 KB", "docx", "2026-04-17")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_files, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_files)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add)

        toolbar.setOnMenuItemClickListener {
            Toast.makeText(requireContext(), it.title, Toast.LENGTH_SHORT).show()
            true
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = FileListAdapter(mockFiles)

        fab.setOnClickListener {
            Toast.makeText(requireContext(), "新建文件夹 / 上传文件", Toast.LENGTH_SHORT).show()
        }
    }
}
